const Stomp = require('stomp-client');

// Variables de estado (Empezamos con frío y mucha luz para que la consola actúe rápido)
let temperatura = 10.0; 
let iluminacion = 950.0;

// Configuración STOMP (mismo puerto 61613 que en Python)
const client = new Stomp('localhost', 61613, 'admin', 'admin');

client.connect(function(sessionId) {
    console.log('Oficina 2 (Node.js) conectada a ActiveMQ...');

    // Suscripción al actuador de temperatura
    client.subscribe('/topic/actuador.temperatura', function(body, headers) {
        let mensaje = JSON.parse(body);
        if (mensaje.oficina === 2) {
            if (mensaje.accion === 'enfriar') temperatura -= 2.0;
            else if (mensaje.accion === 'calentar') temperatura += 2.0;
            console.log(`[OFICINA 2] Orden recibida: ${mensaje.accion} temperatura.`);
        }
    });

    // Suscripción al actuador de iluminación
    client.subscribe('/topic/actuador.iluminacion', function(body, headers) {
        let mensaje = JSON.parse(body);
        if (mensaje.oficina === 2) {
            if (mensaje.accion === 'bajar') iluminacion -= 50.0;
            else if (mensaje.accion === 'subir') iluminacion += 50.0;
            console.log(`[OFICINA 2] Orden recibida: ${mensaje.accion} iluminación.`);
        }
    });

    // Publicación de sensores cada 2 segundos
    setInterval(() => {
        const datosTemp = JSON.stringify({ oficina: 2, valor: parseFloat(temperatura.toFixed(2)) });
        const datosIlum = JSON.stringify({ oficina: 2, valor: parseFloat(iluminacion.toFixed(2)) });

        client.publish('/topic/lecturas.temperatura', datosTemp);
        client.publish('/topic/lecturas.iluminacion', datosIlum);

        console.log(`Enviado -> Temp: ${temperatura.toFixed(2)}°C | Luz: ${iluminacion.toFixed(2)} lm`);

        // Simulamos fluctuación natural
        temperatura += (Math.random() - 0.5);
        iluminacion += (Math.random() * 20 - 10);
    }, 2000);
});