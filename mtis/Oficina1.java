package mtis;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Oficina1 implements MessageListener {
    
    static double temperatura = 26.5;  // Arranca por encima del límite (25°C) → alerta inmediata
    static double iluminacion = 350.0;  // Arranca por debajo del límite (400 lm) → alerta inmediata

    public static void main(String[] args) {
        try {
            // 1. Conexión a ActiveMQ vía OpenWire (JMS)
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // 2. Crear los publicadores (lecturas)
            MessageProducer prodTemp = session.createProducer(session.createTopic("lecturas.temperatura"));
            MessageProducer prodIlum = session.createProducer(session.createTopic("lecturas.iluminacion"));

            // 3. Crear los consumidores (órdenes de la consola)
            MessageConsumer consTemp = session.createConsumer(session.createTopic("actuador.temperatura"));
            MessageConsumer consIlum = session.createConsumer(session.createTopic("actuador.iluminacion"));
            
            // Asignar el listener para modo asíncrono
            Oficina1 listener = new Oficina1();
            consTemp.setMessageListener(listener);
            consIlum.setMessageListener(listener);

            System.out.println("Oficina 1 (Java) conectada y enviando datos...");

            // 4. Bucle infinito publicando datos cada 2 segundos
            while(true) {
                // Formateamos en JSON manualmente para que Python lo entienda sin librerías extra
                String jsonTemp = "{\"oficina\": 1, \"valor\": " + String.format(java.util.Locale.US, "%.2f", temperatura) + "}";
                String jsonIlum = "{\"oficina\": 1, \"valor\": " + String.format(java.util.Locale.US, "%.2f", iluminacion) + "}";
                
                prodTemp.send(session.createTextMessage(jsonTemp));
                prodIlum.send(session.createTextMessage(jsonIlum));
                
                System.out.println("Enviado -> Temp: " + String.format("%.2f", temperatura) + "\u00B0C | Luz: " + String.format("%.2f", iluminacion) + " lm");

                // Fluctuación aleatoria más realista: variación base + pico ocasional (15% de probabilidad)
                double spikeTemp = (Math.random() < 0.15) ? (Math.random() * 5 - 2.5) : 0;
                temperatura += (Math.random() * 2 - 0.8) + spikeTemp;

                double spikeIlum = (Math.random() < 0.15) ? (Math.random() * 120 - 60) : 0;
                iluminacion += (Math.random() * 50 - 25) + spikeIlum;
                
                Thread.sleep(2000);
            }
        } catch(Exception e) { 
            e.printStackTrace(); 
        }
    }

    // 5. Método que reacciona a los mensajes de la Consola Central
    @Override
    public void onMessage(Message msg) {
        try {
            String text = "";
            
            // Comprobamos si nos llega como Texto o como Bytes desde Python
            if (msg instanceof TextMessage) {
                text = ((TextMessage) msg).getText();
            } else if (msg instanceof BytesMessage) {
                BytesMessage bm = (BytesMessage) msg;
                byte[] data = new byte[(int) bm.getBodyLength()];
                bm.readBytes(data);
                text = new String(data, "UTF-8");
            } else {
                return; // Ignoramos silenciosamente formatos desconocidos
            }

            // Parseo manual
            boolean esOficina1 = text.contains("\"oficina\": 1") || text.contains("\"oficina\":1");
            boolean esTemp     = text.contains("\"tipo\": \"temperatura\"") || text.contains("\"tipo\":\"temperatura\"");
            boolean esIlum     = text.contains("\"tipo\": \"iluminacion\"") || text.contains("\"tipo\":\"iluminacion\"");
            boolean esEnfriar  = text.contains("\"enfriar\"");
            boolean esCalentar = text.contains("\"calentar\"");
            boolean esBajar    = text.contains("\"bajar\"");
            boolean esSubir    = text.contains("\"subir\"");

            // Solo actuamos (e imprimimos) si el mensaje es para la Oficina 1
            if (esOficina1) {
                if (esTemp) {
                    if (esEnfriar)       { temperatura -= 2.0; System.out.println("[OFICINA 1] Orden ENFRIAR ejecutada. Temp ahora: " + String.format("%.2f", temperatura) + "\u00B0C"); }
                    else if (esCalentar) { temperatura += 2.0; System.out.println("[OFICINA 1] Orden CALENTAR ejecutada. Temp ahora: " + String.format("%.2f", temperatura) + "\u00B0C"); }
                } else if (esIlum) {
                    if (esBajar)       { iluminacion -= 50; System.out.println("[OFICINA 1] Orden BAJAR LUZ ejecutada. Luz ahora: " + String.format("%.2f", iluminacion) + " lm"); }
                    else if (esSubir)  { iluminacion += 50; System.out.println("[OFICINA 1] Orden SUBIR LUZ ejecutada. Luz ahora: " + String.format("%.2f", iluminacion) + " lm"); }
                }
            }
        } catch(Exception e) {
            System.out.println("[OFICINA 1] Error procesando mensaje: " + e.getMessage());
        }
    }
}