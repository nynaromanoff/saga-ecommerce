import http from 'k6/http';
import { sleep, check } from 'k6';

// 1. Configuração do Cenário: 15 usuários simultâneos por 30 segundos
export const options = {
    vus: 15,          // 15 usuários virtuais simulados em paralelo
    duration: '30s',  // Duração total do ataque de carga
};

export default function () {
    // Alinhe a porta com a porta do seu order-service (Ex: 8080)
    const url = 'http://localhost:8081/api/v1/orders'; 
    
    // Payload do Pedido espelhando o seu OrderRequest e a classe ItemDTO
    const payload = JSON.stringify({
        customerId: 'cliente-load-test-k6',
        items: [
            {
                productSku: 'SWITCH-2', // 🔥 Use um SKU real que exista no seu banco de dados
                quantity: 1
            }
        ]
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // Dispara a requisição POST de criação de pedido
    const res = http.post(url, payload, params);

    // Validação Plena: Garante que a API responde com sucesso (201 Created ou 200 OK)
    check(res, {
        'status é sucesso (2xx)': (r) => r.status === 201 || r.status === 200,
    });

    // Pausa de 200ms entre as requisições para simular usuários rápidos, mas reais
    sleep(0.2);
}