# # SmartPool

En este repositorio vamos a presenta una solución para conseguir la monitorización de las piscinas, 
con un control de pH, temperatura y turbidez del agua, mediante el uso de sensores conectados a 
través de un micro controlador (**ESP32**) y esté se comunicará, o bien a través de wifi con **Firebase**
 mediante una API creada en **nodejs**, alojada en **heroku** o a través de bluetooth low energy con una aplicación **android**.
Además consta de un lector microSD para el caso de no tener wifi poder guardar la monitorización.
## Diseño del circuito con todos sus componentes

![device desing](https://github.com/cr13/smartpool/blob/master/smartpool/Dise%C3%B1o%20del%20circuito.png)