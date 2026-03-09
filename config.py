# Parámetros de conexión a ActiveMQ (Protocolo STOMP)
ACTIVEMQ_HOST = 'localhost'
ACTIVEMQ_PORT = 61613  # Puerto por defecto de STOMP en ActiveMQ
ACTIVEMQ_USER = 'admin'
ACTIVEMQ_PASS = 'admin'

# Nombres de los Topics para publicar lecturas (de Oficinas a Consola)
TOPIC_LECTURAS_TEMP = '/topic/lecturas.temperatura'
TOPIC_LECTURAS_ILUM = '/topic/lecturas.iluminacion'

# Nombres de los Topics para publicar órdenes (de Consola a Oficinas)
TOPIC_ACTUADOR_TEMP = '/topic/actuador.temperatura'
TOPIC_ACTUADOR_ILUM = '/topic/actuador.iluminacion'