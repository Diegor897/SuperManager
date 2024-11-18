import os
from PIL import Image

def redimensionar_imagenes_en_carpeta(carpeta, nuevo_ancho, nuevo_alto):
    """
    Redimensiona todas las imágenes en la carpeta especificada.
    
    :param carpeta: La ruta de la carpeta que contiene las imágenes.
    :param nuevo_ancho: El nuevo ancho deseado para las imágenes redimensionadas.
    :param nuevo_alto: El nuevo alto deseado para las imágenes redimensionadas.
    """
    for archivo in os.listdir(carpeta):
        ruta_completa = os.path.join(carpeta, archivo)
        if os.path.isfile(ruta_completa):
            try:
                imagen = Image.open(ruta_completa)
                if imagen.mode == 'RGBA':
                    imagen = imagen.convert('RGB')
                imagen_redimensionada = imagen.resize((nuevo_ancho, nuevo_alto))
                imagen_redimensionada.save(ruta_completa)
                print(f"Imagen {archivo} redimensionada correctamente.")
            except Exception as e:
                print(f"No se pudo redimensionar la imagen {archivo}: {e}")               

def main():
    """
    Función principal que redimensiona todas las imágenes en las carpetas 'equipos' y 'jugadores'.
    """
    carpeta_equipos = r'C:\Users\elray\Desktop\IW\ProyectoIW_main\SupermanagerIW\src\main\resources\static\img\equipos'
    carpeta_jugadores = r'C:\Users\elray\Desktop\IW\ProyectoIW_main\SupermanagerIW\src\main\resources\static\img\jugadores'
    
    redimensionar_imagenes_en_carpeta(carpeta_equipos, 75, 75)
    redimensionar_imagenes_en_carpeta(carpeta_jugadores, 125, 125)

if __name__ == '__main__':
    main()
