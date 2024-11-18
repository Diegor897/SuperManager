"""
Archivo que mira los últimos partidos y actualiza los datos
de los jugadores almacenados en la BD
"""

import os
from PIL import Image
import requests
from datetime import date, datetime
import sqlite3
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By

meses = {
    'ene': '01',
    'feb': '02',
    'mar': '03',
    'abr': '04',
    'may': '05',
    'sept': '09',
    'oct': '10',
    'nov': '11',
    'dic': '12'
}

posiciones = {
    'BA': 'Base',
    'BA / ES': 'Base',
    'ES / BA': 'Base',
    'ES': 'Alero',
    'ES / AL': 'Alero',
    'AL': 'Alero',
    'AL / AP': 'Alero',
    'AP': 'Pivot',
    'AP / PI': 'Pivot',
    'PI': 'Pivot',
    '-': 'Desconocida'
}

def crear_bd(db_filename):
    """
    Creo la base de datos por si no existe
    """
    conn = sqlite3.connect(db_filename)
    cursor = conn.cursor()
    
    cursor.execute('DROP TABLE IF EXISTS jornada_acb')
    cursor.execute('DROP TABLE IF EXISTS jugador_acb')
    cursor.execute('DROP TABLE IF EXISTS puntos_jugador')

    jornada_acb = f'''CREATE TABLE IF NOT EXISTS jornada_acb (
            local TEXT,
            puntos_local INT,
            visitante TEXT,
            puntos_visitante INT,
            jornada INT,
            PRIMARY KEY (local, visitante, jornada)
        )
    '''

    jugador_acb = f'''CREATE TABLE IF NOT EXISTS jugador_acb (
            nombre TEXT PRIMARY KEY,
            pais TEXT,
            equipo TEXT,
            posicion TEXT,
            partidos_jugados INT,
            puntos_totales INT,
            valoracion_total INT,
            valor_de_mercado REAL
        )
    '''

    puntos_jugador = f'''CREATE TABLE IF NOT EXISTS puntos_jugador (
            nombre TEXT,
            jornada INT,
            puntos INT,
            valoracion INT,
            valor_mercado REAL,
            promedio_puntos REAL,
            valoracion_media REAL,
            PRIMARY KEY (nombre, jornada)
        )
    '''

    cursor.execute(jornada_acb)
    cursor.execute(jugador_acb)
    cursor.execute(puntos_jugador)
    conn.commit()
    conn.close()

def insertar_jornada(db_filename, local, puntos_local, visitante, puntos_visitante, jornada):
    """
    Inserto un nuevo equipo en la base de datos
    """
    conn = sqlite3.connect(db_filename)
    cursor = conn.cursor()

    cursor.execute("INSERT INTO jornada_acb (local, puntos_local, visitante, puntos_visitante, jornada) VALUES (?, ?, ?, ?, ?)"
        , (local, puntos_local, visitante, puntos_visitante, jornada))

    # Guardar los cambios y cerrar la conexión
    conn.commit()
    conn.close()

def insertar_jugador(db_filename, nombre_jugador, pais, equipo, posicion, partidos_jugados, puntos_totales, valoracion_total, valor_de_mercado):
    """
    Inserto un nuevo jugador en la base de datos
    """
    conn = sqlite3.connect(db_filename)
    cursor = conn.cursor()

    cursor.execute("INSERT INTO jugador_acb (nombre, pais, equipo, posicion, partidos_jugados, puntos_totales, valoracion_total, valor_de_mercado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            , (nombre_jugador, pais, equipo, posicion, partidos_jugados, puntos_totales, valoracion_total, valor_de_mercado))

    # Guardar los cambios y cerrar la conexión
    conn.commit()
    conn.close()

def actualizar_info_jugador(db_filename, nombre_jugador, equipo_actual, partidos_jugador, puntos_jugador, valoracion_jugador, valor_mercado):
    """
    Actualizo la información de un jugador en la base de datos
    """
    conn = sqlite3.connect(db_filename)
    cursor = conn.cursor()

    cursor.execute("""
        UPDATE jugador_acb 
        SET equipo = ?, partidos_jugados = ?, puntos_totales = ?, valoracion_total = ?, valor_de_mercado = ?
        WHERE nombre = ?
        """, (equipo_actual, partidos_jugador, puntos_jugador, valoracion_jugador, valor_mercado, nombre_jugador))

    # Guardar los cambios y cerrar la conexión
    conn.commit()
    conn.close()

def guardar_puntos_jugador(db_filename, nombre_jugador, puntos_jugador, valoracion_jugador, valor_mercado, jornada, promedio_puntos, valoracion_media):
    """
    Actualizo la información de un jugador en una jornada en la base de datos
    """
    conn = sqlite3.connect(db_filename)
    cursor = conn.cursor()

    cursor.execute("INSERT INTO puntos_jugador (nombre, jornada, puntos, valoracion, valor_mercado, promedio_puntos, valoracion_media) VALUES (?, ?, ?, ?, ?, ?, ?)"
            , (nombre_jugador, jornada, puntos_jugador, valoracion_jugador, valor_mercado, promedio_puntos, valoracion_media))

    # Guardar los cambios y cerrar la conexión
    conn.commit()
    conn.close()

def get_info_jugador(db_filename, nombre_jugador):
    """
    Obtengo los datos de un jugador
    """
    conn = sqlite3.connect(db_filename)
    cursor = conn.cursor()

    cursor.execute("""
        SELECT equipo, partidos_jugados, puntos_totales, valoracion_total, valor_de_mercado 
        FROM jugador_acb 
        WHERE nombre = ?
    """, (nombre_jugador,))

    result = cursor.fetchone()

    # Guardar los cambios y cerrar la conexión
    conn.commit()
    conn.close()
    
    equipo_actual, partidos_jugador, puntos_jugador, valoracion_jugador, valor_mercado_total = result
    return equipo_actual, partidos_jugador, puntos_jugador, valoracion_jugador, valor_mercado_total

def aceptar_cookies(driver):
    """
    Intenta aceptar las cookies si existen
    """
    # Espero hasta que el iframe esté presente
    WebDriverWait(driver, 3).until(EC.presence_of_element_located((By.XPATH, "//iframe[@title='SP Consent Message']")))
    iframe_cookies = driver.find_element(By.XPATH, "//iframe[@title='SP Consent Message']")
    
    driver.switch_to.frame(iframe_cookies)
    
    # Espero hasta que el botón esté presente
    WebDriverWait(driver, 3).until(EC.presence_of_element_located((By.XPATH, '//button[@title="Aceptar"]')))
    boton_cookies = driver.find_element(By.XPATH, '//button[@title="Aceptar"]')
    
    boton_cookies.click()
    driver.switch_to.default_content()

def convertir_fecha(fecha_str):
    """
    Convierto la fecha en un formato que pueda procesar
    """
    dia, mes, año = fecha_str.split()
    mes_numero = meses[mes.lower()]
    return f'{dia}/{mes_numero}/{año}'

def descargarImgEquipo(driver, nombre_equipo, j):
    img = driver.find_elements(By.CLASS_NAME, "home-game__content__team-stats__team__picture")[j].find_element(By.TAG_NAME, 'img')
    response = requests.get(img.get_attribute('src'))
    nombre_imagen = nombre_equipo + '.jpg'
    with open(os.path.join("equipos", nombre_imagen), "wb") as file:
        file.write(response.content)
        print(f"Imagen guardada como '{nombre_imagen}' en equipos correctamente.")

    # Abre la imagen descargada
    imagen = Image.open(os.path.join("equipos", nombre_imagen))

    if imagen.mode == 'RGBA':
        imagen = imagen.convert('RGB')
    
    # Redimensiona la imagen al tamaño deseado
    imagen = imagen.resize((250, 250))
    
    # Guarda la imagen redimensionada
    imagen.save(os.path.join("equipos", nombre_imagen))
    print(f"Imagen redimensionada a {(300, 300)} y guardada correctamente.")

def descargarImgJugador(driver, nombre_jugador):
    img = driver.find_element(By.CSS_SELECTOR, f"img[alt='{nombre_jugador}']")
    response = requests.get(img.get_attribute('src'))
    nombre_imagen = nombre_jugador + '.jpg'
    with open(os.path.join("jugadores", nombre_imagen), "wb") as file:
        file.write(response.content)
        print(f"Imagen guardada como '{nombre_imagen}' en jugadores correctamente.")

    # Abre la imagen descargada
    imagen = Image.open(os.path.join("jugadores", nombre_imagen))

    if imagen.mode == 'RGBA':
        imagen = imagen.convert('RGB')
    
    # Redimensiona la imagen al tamaño deseado
    imagen = imagen.resize((350, 350))
    
    # Guarda la imagen redimensionada
    imagen.save(os.path.join("jugadores", nombre_imagen))
    print(f"Imagen redimensionada a {(400, 400)} y guardada correctamente.")

def posicion_y_nacionalidad(driver, nombre_jugador, enlace_jugador, ventana_anterior):
    """
    Obtengo la nacionalidad y la posición de un jugador
    """
    driver.execute_script(f"window.open('{enlace_jugador.get_attribute('href')}');")
    driver.switch_to.window(driver.window_handles[-1])
    info_jugador = driver.find_element(By.CLASS_NAME, 'identity__profil').find_elements(By.TAG_NAME, 'li')
    posicion = info_jugador[-1].text
    nacionalidad = info_jugador[1].text

    descargarImgJugador(driver, nombre_jugador)

    driver.close()
    driver._switch_to.window(ventana_anterior)
    
    return nacionalidad, posiciones[posicion]

def main():
    """
    Función principal que navega en busca de la información de la liga
    """
    # Set up Chrome WebDriver with headless option
    options = Options()
    options.headless = True
    driver = webdriver.Chrome(options=options)

    # Amplio la ventana para evitar cualquier error
    driver.maximize_window()

    # Guardo la url de la página principal
    url = 'https://www.proballers.com/es/baloncesto/liga/30/spain-liga-endesa/calendario'

    fecha_actual = date.today()

    os.makedirs("equipos", exist_ok=True)
    os.makedirs("jugadores", exist_ok=True)

    driver.get(url)
    aceptar_cookies(driver)
    
    crear_bd('Liga.db')

    ventana_principal = driver.current_window_handle

    partidos = driver.find_elements(By.XPATH, "//div[contains(@class, 'home-league__schedule__content__entry home-league__schedule__content__boxscore')]//div[contains(@class, 'container-fluid')][2]//table/tbody/tr")

    equipos_liga = {}
    jugadores_liga = set()

    num_partidos = 0 # Variable que se utliza para obtener las jornadas

    for i in range(0, len(partidos), 10):

        rango_partidos = partidos[i:i+10]

        for it in range(len(rango_partidos)):
            info_partido = rango_partidos[it].find_elements(By.XPATH, './td')
            fecha_partido = datetime.strptime(convertir_fecha(info_partido[0].find_element(By.TAG_NAME, 'span').text), "%d/%m/%Y").date()
            link_partido = info_partido[-1].find_element(By.TAG_NAME, 'a')
            local = info_partido[-3].find_element(By.TAG_NAME, 'span').text
            visitante = info_partido[-2].find_element(By.TAG_NAME, 'span').text

            if link_partido.text != 'Game preview':
                
                jornada = num_partidos // 9 + 1
                puntos_equipos = link_partido.text.split('-')
                puntos_equipos = [int(p) for p in puntos_equipos]

                insertar_jornada('Liga.db', local, puntos_equipos[0], visitante, puntos_equipos[1], jornada)

                ultima_fecha = fecha_partido

                driver.execute_script(f"window.open('{link_partido.get_attribute('href')}');")
                driver.switch_to.window(driver.window_handles[-1])

                equipos = driver.find_element(By.CSS_SELECTOR, '.home-game__content__entry.home-game__content__team-stats').find_element(By.CLASS_NAME, 'container-fluid').find_elements(By.CLASS_NAME, 'row')
                for j in range(len(equipos)):
                    nombre_equipo = local if j == 0 else visitante
                    # Si el equipo no ha jugado ningún partido incializo el diccionario
                    if nombre_equipo not in equipos_liga:
                        equipos_liga[nombre_equipo] = 1
                        descargarImgEquipo(driver, nombre_equipo, j)
                    else:
                        equipos_liga[nombre_equipo] += 1

                    info_jugadores = equipos[j].find_element(By.CLASS_NAME,'table').find_element(By.TAG_NAME, 'tbody').find_elements(By.TAG_NAME, 'tr')
                    for k in range(len(info_jugadores)):
                        info_jugador = info_jugadores[k].find_elements(By.TAG_NAME, 'td')
                        enlace_jugador = info_jugador[0].find_element(By.TAG_NAME, 'a')
                        nombre_jugador = enlace_jugador.text
                        jornada = equipos_liga[nombre_equipo]
                        puntos = info_jugador[1].text
                        valoracion = info_jugador[-1].text

                        # Comprobamos que el nombre del jugador es válido
                        if nombre_jugador != "":
                            # Si el jugador es "nuevo" lo añado al conjunto de jugadores de la liga
                            if nombre_jugador not in jugadores_liga:
                                jugadores_liga.add(nombre_jugador)
                                ventana_anterior = driver.current_window_handle
                                nacionalidad, posicion = posicion_y_nacionalidad(driver, nombre_jugador, enlace_jugador, ventana_anterior)
                                insertar_jugador('Liga.db', nombre_jugador, nacionalidad, nombre_equipo, posicion, 0, 0, 0, 50000)
                                # Despues de visitar la página del jugador actualizo las estructuras principales para evitarme errores
                                equipos = driver.find_element(By.CSS_SELECTOR, '.home-game__content__entry.home-game__content__team-stats').find_element(By.CLASS_NAME, 'container-fluid').find_elements(By.CLASS_NAME, 'row')
                                info_jugadores = equipos[j].find_element(By.CLASS_NAME,'table').find_element(By.TAG_NAME, 'tbody').find_elements(By.TAG_NAME, 'tr')

                            equipo_actual, partidos_jugador, puntos_jugador, valoracion_jugador, valor_mercado_total = get_info_jugador('Liga.db', nombre_jugador)
                            
                            if nombre_equipo != equipo_actual:
                                equipo_actual = nombre_equipo
                            
                            partidos_jugador += 1
                            puntos_jugador = puntos_jugador + int(puntos)
                            valoracion_jugador = valoracion_jugador + int(valoracion)
                            
                            promedio_valoracion = valoracion_jugador / partidos_jugador
                            valoracion_media = round((promedio_valoracion), 2)
                            promedio_puntos = round(puntos_jugador / partidos_jugador, 2)
                            valor_mercado = max(round((promedio_puntos * 50000), 2), 50000)

                            if partidos_jugador > 4:
                                valor_mercado_superior = 1.15 * valor_mercado_total
                                valor_mercado_inferior = 0.85 * valor_mercado_total
                                if valor_mercado > valor_mercado_superior:
                                    valor_mercado = round(valor_mercado_superior, 2)
                                elif valor_mercado < valor_mercado_inferior:
                                    valor_mercado = round(valor_mercado_inferior, 2)
                            elif partidos_jugador < 4:
                                valor_mercado = 50000
                            
                            guardar_puntos_jugador('Liga.db', nombre_jugador, int(puntos), int(valoracion), valor_mercado, jornada, promedio_puntos, valoracion_media)
                            actualizar_info_jugador('Liga.db', nombre_jugador, equipo_actual, partidos_jugador, puntos_jugador, valoracion_jugador, valor_mercado)

                # Vuelvo a la página principal y actualizo las estructuras de datos
                driver.close()
                driver._switch_to.window(ventana_principal)
                partidos = driver.find_elements(By.XPATH, "//div[contains(@class, 'home-league__schedule__content__entry home-league__schedule__content__boxscore')]//div[contains(@class, 'container-fluid')][2]//table/tbody/tr")
                rango_partidos = partidos[i:i+10]

            else:
                insertar_jornada('Liga.db', local, 0, visitante, 0, jornada)

            num_partidos = num_partidos + 1

        # Cierro, configurio el driver de nuevoo y lo abro para liberar memoria
        driver.quit()
        driver = webdriver.Chrome(options=options)
        driver.maximize_window()
        driver.get(url)
        aceptar_cookies(driver)
        ventana_principal = driver.current_window_handle
        partidos = driver.find_elements(By.XPATH, "//div[contains(@class, 'home-league__schedule__content__entry home-league__schedule__content__boxscore')]//div[contains(@class, 'container-fluid')][2]//table/tbody/tr")

if __name__ == '__main__':
    main()
