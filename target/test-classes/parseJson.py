import sqlite3
import json

def exportar_tabla_a_json(consulta_sql, nombre_archivo):
    try:
        # Conectarse a la base de datos
        conexion = sqlite3.connect('Liga.db')
        cursor = conexion.cursor()

        # Ejecutar la consulta SQL
        cursor.execute(consulta_sql)
        datos = cursor.fetchall()

        # Convertir los datos a un formato JSON
        datos_json = []
        for fila in datos:
            fila_dict = dict(zip([descripcion[0] for descripcion in cursor.description], fila))
            datos_json.append(fila_dict)

        # Escribir los datos en un archivo JSON
        with open(nombre_archivo, 'w', encoding='utf-8') as file:
            json.dump(datos_json, file, indent=4)

        print(f"Los datos de la tabla han sido exportados a '{nombre_archivo}' correctamente.")
    except sqlite3.Error as error:
        print("Error al interactuar con la base de datos:", error)
    finally:
        if conexion:
            conexion.close()

def main():
    """
    Función principal que navega en busca de la información de la liga
    """
    # Exportar la tabla jugador_acb a JSON
    exportar_tabla_a_json("SELECT * FROM jugador_acb", "jugador_acb.json")

    # Exportar la tabla puntos_jugador a JSON
    exportar_tabla_a_json("SELECT * FROM puntos_jugador", "puntos_jugador.json")

    # Exportar la tabla jornada_acb a JSON
    exportar_tabla_a_json("SELECT * FROM jornada_acb", "jornada_acb.json")

if __name__ == '__main__':
    main()
