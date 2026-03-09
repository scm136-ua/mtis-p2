import stomp
import time
import json
import config

# Valores límite preestablecidos en la consola 
LIMITES = {
    'temp_max': 25.0,
    'temp_min': 20.0,
    'ilum_max': 800.0,
    'ilum_min': 400.0
}

# Almacenamos el último estado conocido de las oficinas
estado_oficinas = {
    1: {'temperatura': None, 'iluminacion': None},
    2: {'temperatura': None, 'iluminacion': None}
}

class ConsolaListener(stomp.ConnectionListener):
    def on_message(self, frame):
        mensaje = json.loads(frame.body)
        oficina = mensaje['oficina']
        valor = mensaje['valor']
        destino = frame.headers['destination']
        
        # Actualizamos el estado según el topic del que provenga el mensaje
        if config.TOPIC_LECTURAS_TEMP in destino:
            estado_oficinas[oficina]['temperatura'] = valor
        elif config.TOPIC_LECTURAS_ILUM in destino:
            estado_oficinas[oficina]['iluminacion'] = valor

# Configuración de conexión
conn = stomp.Connection([(config.ACTIVEMQ_HOST, config.ACTIVEMQ_PORT)])
conn.set_listener('', ConsolaListener())
conn.connect(config.ACTIVEMQ_USER, config.ACTIVEMQ_PASS, wait=True)

# La consola se suscribe a los Topics de lecturas
conn.subscribe(destination=config.TOPIC_LECTURAS_TEMP, id='consola_temp', ack='auto')
conn.subscribe(destination=config.TOPIC_LECTURAS_ILUM, id='consola_ilum', ack='auto')

print("Consola Central iniciada. Escuchando sensores...")

def evaluar_y_actuar():
    """Evalúa los últimos datos recibidos y publica órdenes si es necesario."""
    for ofi, datos in estado_oficinas.items():
        temp = datos['temperatura']
        ilum = datos['iluminacion']
        
        # Control de Temperatura
        if temp is not None:
            if temp > LIMITES['temp_max']:
                orden = json.dumps({"oficina": ofi, "tipo": "temperatura", "accion": "enfriar"})
                conn.send(body=orden, destination=config.TOPIC_ACTUADOR_TEMP)
                print(f"[ALERTA] Ofi {ofi}: Temp Alta ({temp}°C). Orden: enfriar.")
            elif temp < LIMITES['temp_min']:
                orden = json.dumps({"oficina": ofi, "tipo": "temperatura", "accion": "calentar"})
                conn.send(body=orden, destination=config.TOPIC_ACTUADOR_TEMP)
                print(f"[ALERTA] Ofi {ofi}: Temp Baja ({temp}°C). Orden: calentar.")
                
        # Control de Iluminación
        if ilum is not None:
            if ilum > LIMITES['ilum_max']:
                orden = json.dumps({"oficina": ofi, "tipo": "iluminacion", "accion": "bajar"})
                conn.send(body=orden, destination=config.TOPIC_ACTUADOR_ILUM)
                print(f"[ALERTA] Ofi {ofi}: Luz Alta ({ilum} lm). Orden: bajar.")
            elif ilum < LIMITES['ilum_min']:
                orden = json.dumps({"oficina": ofi, "tipo": "iluminacion", "accion": "subir"})
                conn.send(body=orden, destination=config.TOPIC_ACTUADOR_ILUM)
                print(f"[ALERTA] Ofi {ofi}: Luz Baja ({ilum} lm). Orden: subir.")

try:
    while True:
        # La consola recibe información y evalúa cada 5 segundos 
        time.sleep(5)
        print("\n--- Evaluando estado de las oficinas ---")
        evaluar_y_actuar()

except KeyboardInterrupt:
    print("Apagando Consola Central...")
    conn.disconnect()