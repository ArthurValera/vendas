package com.pjt.vendas.controle;

import com.pjt.vendas.modelos.EntradaProd;
import com.pjt.vendas.modelos.ItemEntrada;
import com.pjt.vendas.modelos.Produto;
import com.pjt.vendas.repositorios.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class EntradaControle {
    @Autowired
    private EntradaRep entradaRep;
    @Autowired
    private ItemEntradaRep itemEntradaRep;
    @Autowired
    private ProdutoRep produtoRep;
    @Autowired
    private FuncionarioRep funcionarioRep;
    @Autowired
    private FornecedorRep fornecedorRep;

    private List<ItemEntrada> listaItemEntrada = new ArrayList<ItemEntrada>();

    @GetMapping("/cadastraEntradaProd")
    public ModelAndView cadastrar(EntradaProd entradaProd, ItemEntrada itemEntrada){
        ModelAndView mv = new ModelAndView("/adm/entradaProds/cadastro");
        mv.addObject("entradaProd", entradaProd);
        mv.addObject("itemEntrada", itemEntrada);
        mv.addObject("listaItemEntrada", this.listaItemEntrada);
        mv.addObject("listaFuncionarios", funcionarioRep.findAll());
        mv.addObject("listaFornecedores", fornecedorRep.findAll());
        mv.addObject("listaProdutos", produtoRep.findAll());
        return mv;
    }

    @GetMapping("/listaEntradaProd")
    public ModelAndView listar(){
        ModelAndView mv = new ModelAndView("/adm/entradaProds/lista");
        mv.addObject("listaEntradaProd", entradaRep.findAll());
        return mv;
    }

    @GetMapping("/editaEntradaProd/{id}")
    public ModelAndView editar(@PathVariable("id") Long id) {
        Optional<EntradaProd> entradaProd = entradaRep.findById(id);
        this.listaItemEntrada = itemEntradaRep.buscarPorEntrada(id);
        return cadastrar(entradaProd.get(), new ItemEntrada());
    }
    @GetMapping("/removeEntradaProd/{id}")
    public ModelAndView remover(@PathVariable("id") Long id) {
        Optional<EntradaProd> entradaProd = entradaRep.findById(id);

        if (entradaProd.isPresent()) {
            // Remover os itens relacionados primeiro
            List<ItemEntrada> itens = itemEntradaRep.buscarPorEntrada(id);
            for (ItemEntrada item : itens) {
                itemEntradaRep.delete(item);
            }

            // Agora, remover a entrada principal
            entradaRep.delete(entradaProd.get());
        }

        return listar();
    }


    @PostMapping("/salvaEntradaProd")
    public ModelAndView salvar(String acao, EntradaProd entradaProd, ItemEntrada itemEntrada, BindingResult result) {
        if (result.hasErrors()) {
            return cadastrar(entradaProd, itemEntrada);
        }

        if (acao.equals("itens")) {
            // Adiciona o item à lista e atualiza os totais
            this.listaItemEntrada.add(itemEntrada);
            entradaProd.setValorTotal(entradaProd.getValorTotal() + (itemEntrada.getPreco() * itemEntrada.getQuantidade()));
            entradaProd.setQuantidadeTotal(entradaProd.getQuantidadeTotal() + itemEntrada.getQuantidade());
        } else if (acao.equals("salvar")) {
            // Salva a entrada e seus itens
            entradaRep.saveAndFlush(entradaProd);

            for (ItemEntrada it : listaItemEntrada) {
                it.setEntradaProd(entradaProd);
                itemEntradaRep.saveAndFlush(it);

                // Atualiza o estoque e valores do produto
                Optional<Produto> prod = produtoRep.findById(it.getProduto().getId());
                if (prod.isPresent()) {
                    Produto produto = prod.get();
                    produto.setEstoque(produto.getEstoque() + it.getQuantidade());
                    produto.setPreco(it.getPreco());
                    produto.setCusto(it.getCusto());
                    produtoRep.saveAndFlush(produto);
                }
            }

            // Limpa a lista de itens após salvar
            this.listaItemEntrada = new ArrayList<>();
            return cadastrar(new EntradaProd(), new ItemEntrada());
        }

        return cadastrar(entradaProd, new ItemEntrada());
    }

    public List<ItemEntrada> getListaItemEntrada() {
        return listaItemEntrada;
    }

    public void setListaItemEntrada(List<ItemEntrada> listaItemEntrada) {
        this.listaItemEntrada = listaItemEntrada;
    }
}
