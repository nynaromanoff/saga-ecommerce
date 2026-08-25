# Saga Coreografada E-Commerce - Arquitetura de Microsserviços

Este repositório contém um ecossistema distribuído de e-commerce construído em **Java 21** com **Spring Boot 4**, modelado sob a arquitetura de **Microsserviços Descentralizados** e orientado a eventos através do padrão **SAGA Coreografada**.

O projeto foi projetado com foco em alta resiliência, consistência eventual de dados, isolamento de domínios e observabilidade avançada.

## 🚀 Arquitetura e Fluxo do Sistema

O ecossistema é composto por 4 microsserviços autônomos que se comunicam de forma assíncrona utilizando **RabbitMQ (Fanout Exchanges dedicadas)** para evitar pontos únicos de falha e acoplamento de rede:

1. **`product-service`**: Gerencia o catálogo de produtos e realiza o upload físico de imagens para a nuvem utilizando **AWS S3 / LocalStack**. Ao salvar um produto, publica o evento na exchange `product.v1.product-created`.
2. **`inventory-service`**: Possui dois listeners blindados. Um sincroniza a criação do produto inicializando o estoque. O outro processa a baixa física de mercadorias a partir de novos pedidos ou executa **Sagas Compensatórias** de estorno em caso de falha.
3. **`order-service`**: Centraliza o fluxo de compras utilizando identificadores únicos **UUID**. Valida os produtos de forma síncrona via `RestClient` e publica intenções de compra na exchange `order.v1.order-created`.
4. **`payment-service`**: Consome os eventos de pedido de forma assíncrona, persiste o histórico financeiro e realiza a integração real com o gateway de pagamento externo **Stripe (API Payment Intents)** para validação de cartões de crédito.

---

## 🛠️ Tecnologias e Padrões Utilizados

*   **Core:** Java 21 & Spring Boot 4
*   **Data Tier:** PostgreSQL (Bancos de dados isolados por serviço / Database-per-service)
*   **Messaging:** RabbitMQ (Mensageria assíncrona com Fanout Exchanges independentes)
*   **Cloud & Storage:** AWS SDK v2, Amazon S3 & LocalStack (Simulação local de Cloud)
*   **Payment Gateway:** Stripe API (Ambiente Sandbox / Cartões de Crédito)
*   **Observability:** Prometheus (Coleta de métricas via Spring Actuator/Micrometer) & Grafana (Dashboards de tráfego HTTP e saúde da JVM)
*   **Performance Testing:** Grafana k6 (Scripts em JavaScript para testes de estresse de alta concorrência)

---

## 📉 Resiliência e Mecanismos de Salvaguarda (Padrão SAGA)

O sistema foi blindado contra falhas comuns em sistemas distribuídos através de:
*   **Saga Compensatória Automatizada:** Caso o `payment-service` receba uma negação da Stripe (cartão recusado), o `order-service` transiciona o status do pedido para `CANCELED` e notifica o `inventory-service` para **estornar e devolver** imediatamente as quantidades reservadas ao estoque, mantendo a consistência do banco.
*   **Idempotência e Prevenção de Falhas Críticas:** Implementação de checagens preventivas (`findBySkuIgnoreCase`) e validações de payloads (`jakarta.validation`) que bloqueiam efeitos colaterais como estoques negativos e `NullPointerException` causados por falhas de rede.
*   **Inicialização Ativa de Infraestrutura:** Configuração de Beans auto-declarativos via `RabbitAdmin` para forçar a criação física de filas e amarrações no broker antes da ativação dos Listeners, eliminando condições de corrida (*race conditions*) no boot das aplicações.

---

## 📊 Testes de Carga e Monitoramento

O ecossistema foi submetido a testes de estresse utilizando o **k6** para validar o comportamento sob alta concorrência, simulando múltiplos usuários efetuando compras simultaneamente.

### Como rodar o teste de carga:
1. Certifique-se de que o k6 está instalado nativamente e execute na pasta `/tests`:
   ```bash
   k6 run load-test.js
   ```

### Métricas monitoradas no Grafana:
*   **`http_server_requests_seconds_count`**: Gráficos de linhas medindo a vazão de requisições por segundo e identificando picos de tráfego em tempo real.
*   **JVM Micrometer (ID 11378)**: Dashboard completo monitorando consumo de memória RAM Heap, Threads ativas e uso de CPU durante os picos do k6.

<img width="728" height="532" alt="grafana" src="https://github.com/user-attachments/assets/8c332482-5388-4eeb-860c-d9af44ebbb54" />
