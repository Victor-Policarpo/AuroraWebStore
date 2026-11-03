# AuroraWebStore

> Backend em desenvolvimento para uma plataforma de comércio digital, com foco inicial em catálogo administrativo de jogos e produtos digitais.

O AuroraWebStore organiza os fundamentos de uma loja digital: autenticação, administração de produtos e categorias, mídia de catálogo e enriquecimento de metadados de jogos. A base também modela conceitos comerciais como carrinho, pedidos, pagamentos, cupons, chaves digitais e fornecedores, que representam espaço de evolução do produto.

O repositório está em desenvolvimento. O escopo disponível concentra-se no backend e ainda não representa uma experiência de compra completa para o cliente.

## Visão do produto

A proposta é oferecer uma base para administrar um catálogo de produtos digitais, especialmente jogos, mantendo separados os metadados descritivos do produto e as informações necessárias à sua comercialização. A consulta à IGDB ajuda a localizar informações de jogos; a decisão de publicar um item para venda continua sendo uma etapa administrativa, e a existência de metadados não significa que haja licença ou estoque comercial disponível.

O projeto também prepara o domínio para evoluir a partir do catálogo: imagens associadas aos produtos, fornecedores e ofertas, inventário de chaves e os conceitos de carrinho, pedido e pagamento já aparecem no modelo de dados. Esses elementos devem ser entendidos como estrutura de domínio, não como fluxos de compra já entregues.

## Capacidades presentes no código

- **Contas e autenticação:** cadastro de usuários com papel inicial de cliente, senha protegida com BCrypt e autenticação por token JWT.
- **Administração do catálogo:** criação, consulta paginada, atualização parcial e mudança de estado de produtos e categorias em rotas administrativas.
- **Estados de produto:** rascunho, ativo, inativo e sem estoque. Novos produtos são criados como rascunho.
- **Metadados de jogos:** integração administrativa com a IGDB para pesquisar jogos e obter detalhes para um fluxo de importação. O serviço de importação existente cria um produto em rascunho, sujeito às condições codificadas atualmente.
- **Imagens e mídia:** associação de arquivos a produtos, armazenamento no Amazon S3, listagem por URL pré-assinada e remoção de imagens. O código aceita formatos de imagem e também declara suporte a MP4/WebM.
- **Contratos HTTP:** DTOs de requisição e resposta, validação de entrada, tratamento centralizado de erros e dependência OpenAPI.
- **Persistência evolutiva:** esquema PostgreSQL gerenciado por migrações Flyway.

As operações observadas de catálogo, imagens e integração IGDB são administrativas. Não foi identificada uma API pública de navegação de produtos.

## Modelo de domínio

O modelo relacional contempla:

- **Identidade:** usuários, papéis e carrinho individual.
- **Catálogo:** categorias, produtos e imagens.
- **Compra:** itens de carrinho, pedidos, itens de pedido e pagamentos.
- **Promoções:** cupons e registro de utilização.
- **Entrega digital:** chaves de produto com estados de disponibilidade, reserva e venda.
- **Abastecimento:** fornecedores e ofertas relacionadas a produtos.

Preços e valores monetários são representados como inteiros (`BIGINT` no PostgreSQL e `Long` no modelo Java), com moeda registrada separadamente. As migrações incluem restrições para unicidade e consistência em partes importantes do domínio. A descrição completa do esquema pode ser consultada em [docs/er-diagram.md](docs/er-diagram.md).

## Arquitetura e tecnologias

O projeto adota um backend monolítico organizado em camadas. Controllers recebem requisições HTTP; serviços concentram os casos de uso; repositórios e entidades fazem a persistência; DTOs e mappers isolam o contrato da API do modelo interno.

| Área | Tecnologia ou abordagem |
|---|---|
| Linguagem | Java 21 |
| Aplicação web | Spring Boot 4.1.1 e Spring MVC |
| API | REST, DTOs, validação e OpenAPI |
| Persistência | Spring Data JPA, Hibernate e PostgreSQL |
| Evolução do banco | Flyway |
| Segurança | Spring Security, OAuth2 Resource Server e JWT |
| Mapeamento | MapStruct |
| Imagens | AWS SDK para Amazon S3 |
| Dados de jogos | IGDB com autenticação Twitch OAuth |
| Build e dependências | Maven |

O código principal está em `backend/commerce/src/main/java/com/webstore/commerce`, dividido em `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`, `security`, `integration` e `exception`.

## Segurança e integrações

As rotas administrativas usam autorização baseada na autoridade `SCOPE_ADMIN`. O cadastro público atribui o papel de cliente, evitando que o próprio usuário escolha privilégios elevados no contrato de cadastro. O projeto também prevê integração com credenciais externas configuradas por ambiente para banco de dados, AWS e IGDB.

O armazenamento de imagens e a persistência no banco são sistemas separados. Portanto, a consistência entre esses serviços depende de tratamento de falhas na aplicação. As integrações comerciais de pagamento, e-mail e APIs de fornecedores não estão demonstradas como fluxos implementados neste repositório.

## Estado e limites atuais

O AuroraWebStore está em construção. No estado observado, há fundamentos de autenticação, administração de catálogo, mídia e pesquisa/importação de dados de jogos. O modelo de dados prevê uma operação de loja mais ampla, mas o repositório não contém, até onde foi identificado, frontend nem APIs de cliente para catálogo público, carrinho, checkout, pagamentos, cupons, gestão de fornecedores ou entrega de chaves.

A importação IGDB presente no working tree ainda tem decisões fixas de categoria, plataforma, região e preço, e requer a categoria esperada para concluir. Ela deve ser tratada como fluxo inicial, ainda sujeito a refinamento, e não como importador comercial genérico. O README descreve capacidades identificadas no código, sem afirmar que integrações externas foram validadas em ambiente real.

## Organização do repositório

```text
.
├── backend/
│   └── commerce/       # Aplicação Spring Boot, migrações e testes
└── docs/
    └── er-diagram.md   # Visão relacional do domínio
```

## Documentação relacionada

- [Relatório de análise do projeto](docs/relatorio-projeto.md)
- [Diagrama e regras do modelo de dados](docs/er-diagram.md)
- A base de conhecimento e as decisões registradas no Obsidian foram consultadas durante a análise; esse material permanece no vault do projeto, fora deste repositório.

## Público e objetivo

Este projeto é voltado a demonstrar e evoluir uma arquitetura de backend para comércio digital: da administração do catálogo e integração com metadados de jogos até os componentes de domínio necessários a uma operação de venda de produtos digitais.
