'use strict';

/*
 * FocusLeaf — interface web.
 * JavaScript puro, sem frameworks. Todo texto vindo da API é inserido com textContent,
 * nunca como HTML, para evitar injeção de código.
 */

const CHAVE_LEITOR = 'focusleaf.leitor';
const CHAVE_FONTE = 'focusleaf.tamanhoTexto';
const FONTE_MINIMA = 16;
const FONTE_MAXIMA = 26;

const estado = {
    leitor: '',
    leitura: null,      // { conteudoId, titulo, sessao, bloco }
    cronometro: null,
    tamanhoTexto: 19,
};

const $ = (id) => document.getElementById(id);
const codificar = encodeURIComponent;

/* ---------- Utilidades ---------- */

function elemento(tag, propriedades = {}, ...filhos) {
    const el = document.createElement(tag);
    for (const [chave, valor] of Object.entries(propriedades)) {
        if (valor === undefined || valor === null || valor === false) continue;
        if (chave === 'class') el.className = valor;
        else if (chave === 'text') el.textContent = valor;
        else if (chave.startsWith('on')) el.addEventListener(chave.slice(2), valor);
        else el.setAttribute(chave, valor === true ? '' : valor);
    }
    for (const filho of filhos) {
        if (filho !== null && filho !== undefined) el.append(filho);
    }
    return el;
}

function lerArmazenado(chave) {
    try {
        return localStorage.getItem(chave);
    } catch {
        return null;
    }
}

function guardar(chave, valor) {
    try {
        localStorage.setItem(chave, valor);
    } catch {
        // Sem armazenamento local (modo privado, por exemplo): segue sem lembrar.
    }
}

function plural(quantidade, singular, pluralTexto) {
    return `${quantidade} ${quantidade === 1 ? singular : pluralTexto}`;
}

function paraMilissegundos(instante) {
    if (typeof instante === 'number') return instante * 1000;
    if (typeof instante !== 'string') return NaN;
    // Reduz frações de segundo a 3 casas para qualquer navegador aceitar.
    return Date.parse(instante.replace(/(\.\d{3})\d+/, '$1'));
}

function relogio(segundos) {
    const minutos = Math.floor(segundos / 60);
    const resto = segundos % 60;
    return `${String(minutos).padStart(2, '0')}:${String(resto).padStart(2, '0')}`;
}

function duracaoPorExtenso(segundos) {
    if (segundos < 60) return plural(segundos, 'segundo', 'segundos');
    const minutos = Math.floor(segundos / 60);
    const resto = segundos % 60;
    const textoMinutos = plural(minutos, 'minuto', 'minutos');
    return resto === 0 ? textoMinutos : `${textoMinutos} e ${plural(resto, 'segundo', 'segundos')}`;
}

/* ---------- Comunicação com a API ---------- */

async function api(caminho, opcoes = {}) {
    const configuracao = { method: opcoes.metodo || 'GET', headers: { Accept: 'application/json' } };
    if (opcoes.corpo !== undefined) {
        configuracao.headers['Content-Type'] = 'application/json';
        configuracao.body = JSON.stringify(opcoes.corpo);
    }

    let resposta;
    try {
        resposta = await fetch(caminho, configuracao);
    } catch {
        throw new Error('Não foi possível falar com o servidor. Verifique se a aplicação está rodando.');
    }

    if (resposta.status === 204) return null;
    const dados = await resposta.json().catch(() => null);

    if (!resposta.ok) {
        const erro = new Error(dados?.mensagem || `A requisição falhou (HTTP ${resposta.status}).`);
        erro.status = resposta.status;
        erro.campos = dados?.campos;
        throw erro;
    }
    return dados;
}

/* ---------- Avisos e navegação entre telas ---------- */

let temporizadorAviso;

function avisar(mensagem, tipo = 'info') {
    const aviso = $('aviso');
    aviso.textContent = mensagem;
    aviso.dataset.tipo = tipo;
    aviso.hidden = false;
    clearTimeout(temporizadorAviso);
    temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 6000);
}

function mostrarTela(nome) {
    for (const tela of ['biblioteca', 'leitura', 'resumo']) {
        $(`tela-${tela}`).hidden = tela !== nome;
    }
    document.body.dataset.tela = nome;
    $('aviso').hidden = true;
    window.scrollTo(0, 0);
}

/* ---------- Caule: uma folha por bloco ---------- */

function preencherCaule(container, total, lidos, atual) {
    const conjuntoLidos = new Set(lidos);
    const folhas = [];
    for (let ordem = 1; ordem <= total; ordem++) {
        let classe = 'folhinha';
        if (conjuntoLidos.has(ordem)) classe += ' lida';
        if (ordem === atual) classe += ' atual';
        folhas.push(elemento('span', { class: classe }));
    }
    container.replaceChildren(...folhas);
    return container;
}

/* ---------- Ambiente (local, staging, produção) ---------- */

async function carregarAmbiente() {
    try {
        const info = await api('/api/info');
        const selo = $('ambiente');
        selo.textContent = `${info.ambiente}, versão ${info.versao}`;
        selo.dataset.ambiente = info.ambiente;
        selo.title = `Commit ${info.commit}`;
        selo.hidden = false;
    } catch {
        // O selo é apenas informativo.
    }
}

/* ---------- Leitor ---------- */

function definirLeitor(valor) {
    estado.leitor = valor.trim().toLowerCase();
    $('campo-leitor').value = estado.leitor;
    guardar(CHAVE_LEITOR, estado.leitor);
}

function exigirLeitor() {
    if (!estado.leitor && $('campo-leitor').value.trim()) {
        definirLeitor($('campo-leitor').value);
    }
    if (estado.leitor) return true;
    avisar('Escolha um apelido de leitor antes de começar a ler.', 'erro');
    $('campo-leitor').focus();
    return false;
}

function iniciarLeitor() {
    estado.leitor = lerArmazenado(CHAVE_LEITOR) || '';
    $('campo-leitor').value = estado.leitor;

    $('form-leitor').addEventListener('submit', (evento) => {
        evento.preventDefault();
        if (!$('campo-leitor').value.trim()) {
            avisar('Digite um apelido para guardar seu progresso.', 'erro');
            return;
        }
        definirLeitor($('campo-leitor').value);
        avisar(`Apelido salvo. Você está lendo como ${estado.leitor}.`);
        carregarConteudos();
    });
}

/* ---------- Biblioteca ---------- */

async function carregarConteudos() {
    let conteudos;
    try {
        conteudos = await api('/api/conteudos');
    } catch (erro) {
        avisar(erro.message, 'erro');
        return;
    }

    const progressos = await Promise.all(conteudos.map((conteudo) => (
        estado.leitor
            ? api(`/api/progresso?leitor=${codificar(estado.leitor)}&conteudoId=${codificar(conteudo.id)}`)
                .catch(() => null)
            : Promise.resolve(null)
    )));

    $('lista-vazia').hidden = conteudos.length > 0;
    $('lista-conteudos').replaceChildren(
        ...conteudos.map((conteudo, indice) => itemConteudo(conteudo, progressos[indice]))
    );
}

function itemConteudo(conteudo, progresso) {
    const meta = [];
    if (conteudo.autor) meta.push(conteudo.autor);
    meta.push(plural(conteudo.totalBlocos, 'bloco', 'blocos'));
    meta.push(`cerca de ${conteudo.tempoEstimadoMinutos} min de leitura`);

    let rotulo = 'Começar a ler';
    if (progresso?.sessaoEmAndamentoId) rotulo = 'Continuar leitura';
    else if (progresso?.percentualConcluido === 100) rotulo = 'Ler de novo';

    let andamento = null;
    if (progresso && progresso.blocosConcluidos.length > 0) {
        andamento = elemento('div', { class: 'andamento' },
            preencherCaule(elemento('div', { class: 'caule mini', 'aria-hidden': 'true' }),
                progresso.totalBlocos, progresso.blocosConcluidos, null),
            elemento('span', {
                text: `${progresso.blocosConcluidos.length} de ${plural(progresso.totalBlocos, 'bloco lido', 'blocos lidos')}`,
            }));
    }

    return elemento('li', { class: 'item' },
        elemento('div', {},
            elemento('h3', { text: conteudo.titulo }),
            elemento('p', { class: 'meta', text: meta.join(', ') }),
            conteudo.descricao ? elemento('p', { class: 'descricao', text: conteudo.descricao }) : null,
            andamento),
        elemento('div', { class: 'item-acoes' },
            elemento('button', {
                type: 'button', class: 'primario', text: rotulo,
                onclick: () => abrirLeitura(conteudo.id),
            }),
            elemento('button', {
                type: 'button', class: 'discreto', text: 'Excluir',
                'aria-label': `Excluir ${conteudo.titulo}`,
                onclick: () => excluirConteudo(conteudo),
            })));
}

async function excluirConteudo(conteudo) {
    if (!window.confirm(`Excluir "${conteudo.titulo}"? O histórico de leitura continua guardado.`)) return;
    try {
        await api(`/api/conteudos/${codificar(conteudo.id)}`, { metodo: 'DELETE' });
        avisar('Texto excluído.');
        carregarConteudos();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

/* ---------- Cadastro de texto ---------- */

/**
 * Divide o texto em blocos usando linhas em branco como separador.
 * Se a primeira linha de um trecho começar com "#", ela vira o título do bloco.
 */
function separarBlocos(textoCompleto) {
    return textoCompleto
        .split(/\n\s*\n/)
        .map((trecho) => trecho.trim())
        .filter((trecho) => trecho.length > 0)
        .map((trecho) => {
            const linhas = trecho.split('\n');
            if (linhas[0].startsWith('#')) {
                return {
                    titulo: linhas[0].replace(/^#+\s*/, '').trim() || null,
                    texto: linhas.slice(1).join('\n').trim(),
                };
            }
            return { titulo: null, texto: trecho };
        });
}

function atualizarContagem() {
    const quantidade = separarBlocos($('campo-texto').value).length;
    $('contagem-blocos').textContent = quantidade === 0
        ? 'Nenhum bloco ainda.'
        : `Será salvo em ${plural(quantidade, 'bloco', 'blocos')}.`;
}

function abrirFormulario() {
    $('form-conteudo').hidden = false;
    $('botao-novo').hidden = true;
    $('campo-titulo').focus();
}

function fecharFormulario() {
    $('form-conteudo').reset();
    $('form-conteudo').hidden = true;
    $('erros-formulario').hidden = true;
    $('botao-novo').hidden = false;
    atualizarContagem();
}

function mostrarErrosFormulario(erro) {
    const mensagens = erro.campos ? [...new Set(Object.values(erro.campos))] : [erro.message];
    $('erros-formulario').replaceChildren(...mensagens.map((mensagem) => elemento('li', { text: mensagem })));
    $('erros-formulario').hidden = false;
}

function iniciarCadastro() {
    $('botao-novo').addEventListener('click', abrirFormulario);
    $('botao-cancelar').addEventListener('click', fecharFormulario);
    $('campo-texto').addEventListener('input', atualizarContagem);

    $('form-conteudo').addEventListener('submit', async (evento) => {
        evento.preventDefault();
        const corpo = {
            titulo: $('campo-titulo').value,
            autor: $('campo-autor').value.trim() || null,
            descricao: $('campo-descricao').value.trim() || null,
            blocos: separarBlocos($('campo-texto').value),
        };
        try {
            const criado = await api('/api/conteudos', { metodo: 'POST', corpo });
            fecharFormulario();
            avisar(`"${criado.titulo}" foi salvo em ${plural(criado.totalBlocos, 'bloco', 'blocos')}.`);
            carregarConteudos();
        } catch (erro) {
            mostrarErrosFormulario(erro);
        }
    });
}

/* ---------- Leitura ---------- */

async function abrirLeitura(conteudoId) {
    if (!exigirLeitor()) return;
    try {
        const progresso = await api(
            `/api/progresso?leitor=${codificar(estado.leitor)}&conteudoId=${codificar(conteudoId)}`);

        // Retoma a sessão aberta, se existir; senão, começa uma nova.
        const sessao = progresso.sessaoEmAndamentoId
            ? await api(`/api/sessoes/${codificar(progresso.sessaoEmAndamentoId)}`)
            : await api('/api/sessoes', { metodo: 'POST', corpo: { conteudoId, leitor: estado.leitor } });

        estado.leitura = { conteudoId, titulo: progresso.tituloConteudo, sessao, bloco: null };
        $('leitura-conteudo').textContent = progresso.tituloConteudo;
        await mostrarBloco(sessao.blocoAtual);
        mostrarTela('leitura');
        $('leitura-titulo-bloco').focus({ preventScroll: true });
        iniciarCronometro();
    } catch (erro) {
        estado.leitura = null;
        avisar(erro.message, 'erro');
    }
}

async function mostrarBloco(ordem) {
    const leitura = estado.leitura;
    const bloco = await api(`/api/conteudos/${codificar(leitura.conteudoId)}/blocos/${ordem}`);
    leitura.bloco = bloco;

    const lidos = leitura.sessao.blocosConcluidos;
    $('leitura-posicao').textContent = `Bloco ${bloco.ordem} de ${bloco.totalBlocos}`;
    preencherCaule($('caule'), leitura.sessao.totalBlocos, lidos, bloco.ordem);
    $('leitura-titulo-bloco').textContent = bloco.titulo || `Bloco ${bloco.ordem}`;
    $('leitura-texto').textContent = bloco.texto;
    $('leitura-estimativa').textContent =
        `${plural(bloco.palavras, 'palavra', 'palavras')}, cerca de ${bloco.tempoEstimadoMinutos} min`;

    $('botao-anterior').disabled = bloco.anterior === null;
    const jaLido = lidos.includes(bloco.ordem);
    if (bloco.proximo === null) {
        $('botao-concluir').textContent = 'Concluir leitura';
    } else {
        $('botao-concluir').textContent = jaLido ? 'Ir para o próximo bloco' : 'Concluir bloco';
    }
    window.scrollTo(0, 0);
}

function primeiroBlocoNaoLido(sessao) {
    for (let ordem = 1; ordem <= sessao.totalBlocos; ordem++) {
        if (!sessao.blocosConcluidos.includes(ordem)) return ordem;
    }
    return sessao.totalBlocos;
}

async function concluirBlocoAtual() {
    const leitura = estado.leitura;
    if (!leitura?.bloco) return;
    const botao = $('botao-concluir');
    botao.disabled = true;
    try {
        const sessao = await api(
            `/api/sessoes/${codificar(leitura.sessao.id)}/blocos/${leitura.bloco.ordem}/concluir`,
            { metodo: 'POST' });
        leitura.sessao = sessao;

        if (sessao.status === 'CONCLUIDA') {
            mostrarResumo(sessao);
            return;
        }
        await mostrarBloco(leitura.bloco.proximo ?? primeiroBlocoNaoLido(sessao));
        $('leitura-titulo-bloco').focus({ preventScroll: true });
    } catch (erro) {
        avisar(erro.message, 'erro');
    } finally {
        botao.disabled = false;
    }
}

async function voltarBloco() {
    const leitura = estado.leitura;
    if (!leitura?.bloco || leitura.bloco.anterior === null) return;
    try {
        await mostrarBloco(leitura.bloco.anterior);
        $('leitura-titulo-bloco').focus({ preventScroll: true });
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

function pausarLeitura() {
    pararCronometro();
    estado.leitura = null;
    mostrarTela('biblioteca');
    avisar('Leitura pausada. Ela continua de onde você parou.');
    carregarConteudos();
}

async function encerrarLeitura() {
    const leitura = estado.leitura;
    if (!leitura) return;
    if (!window.confirm('Encerrar esta leitura agora? O bloco em que você parou fica registrado.')) return;
    try {
        const sessao = await api(`/api/sessoes/${codificar(leitura.sessao.id)}/abandonar`, { metodo: 'POST' });
        mostrarResumo(sessao);
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

/* ---------- Cronômetro e tamanho do texto ---------- */

function iniciarCronometro() {
    pararCronometro();
    const atualizar = () => {
        const inicio = paraMilissegundos(estado.leitura?.sessao?.iniciadaEm);
        if (Number.isNaN(inicio)) return;
        const segundos = Math.max(0, Math.floor((Date.now() - inicio) / 1000));
        $('cronometro').textContent = relogio(segundos);
    };
    atualizar();
    estado.cronometro = setInterval(atualizar, 1000);
}

function pararCronometro() {
    clearInterval(estado.cronometro);
    estado.cronometro = null;
}

function aplicarTamanhoTexto(tamanho) {
    estado.tamanhoTexto = Math.min(FONTE_MAXIMA, Math.max(FONTE_MINIMA, tamanho));
    document.documentElement.style.setProperty('--tamanho-texto', `${estado.tamanhoTexto}px`);
    $('fonte-menor').disabled = estado.tamanhoTexto <= FONTE_MINIMA;
    $('fonte-maior').disabled = estado.tamanhoTexto >= FONTE_MAXIMA;
    guardar(CHAVE_FONTE, String(estado.tamanhoTexto));
}

/* ---------- Resumo ---------- */

function mostrarResumo(sessao) {
    pararCronometro();
    const concluida = sessao.status === 'CONCLUIDA';

    $('resumo-conteudo').textContent = estado.leitura?.titulo || '';
    $('resumo-titulo').textContent = concluida
        ? 'Leitura concluída'
        : `Leitura encerrada no bloco ${sessao.blocoAbandono}`;
    preencherCaule($('resumo-caule'), sessao.totalBlocos, sessao.blocosConcluidos, null);

    const metricas = [
        ['Tempo de leitura', duracaoPorExtenso(sessao.duracaoSegundos)],
        ['Blocos lidos', `${sessao.blocosConcluidos.length} de ${sessao.totalBlocos}`],
        ['Palavras lidas', sessao.palavrasLidas.toLocaleString('pt-BR')],
        ['Velocidade', `${sessao.velocidadePalavrasPorMinuto} palavras por minuto`],
    ];
    $('resumo-metricas').replaceChildren(...metricas.flatMap(([nome, valor]) => [
        elemento('dt', { text: nome }),
        elemento('dd', { text: valor }),
    ]));

    estado.leitura = null;
    mostrarTela('resumo');
    $('resumo-titulo').focus({ preventScroll: true });
}

/* ---------- Início ---------- */

function iniciar() {
    iniciarLeitor();
    iniciarCadastro();

    const tamanhoGuardado = Number(lerArmazenado(CHAVE_FONTE));
    aplicarTamanhoTexto(Number.isFinite(tamanhoGuardado) && tamanhoGuardado > 0 ? tamanhoGuardado : 19);
    $('fonte-menor').addEventListener('click', () => aplicarTamanhoTexto(estado.tamanhoTexto - 1));
    $('fonte-maior').addEventListener('click', () => aplicarTamanhoTexto(estado.tamanhoTexto + 1));

    $('botao-concluir').addEventListener('click', concluirBlocoAtual);
    $('botao-anterior').addEventListener('click', voltarBloco);
    $('botao-pausar').addEventListener('click', pausarLeitura);
    $('botao-encerrar').addEventListener('click', encerrarLeitura);
    $('botao-voltar').addEventListener('click', () => {
        mostrarTela('biblioteca');
        carregarConteudos();
    });

    carregarAmbiente();
    carregarConteudos();
}

iniciar();
