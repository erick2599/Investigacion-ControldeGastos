# Reglas de R8/ProGuard para Control de Gastos.

# Firestore usa reflexión para convertir documentos a los modelos. Necesita el
# constructor sin argumentos y los getters públicos, que son los que definen las
# propiedades (`getTitulo` -> "titulo"). Si R8 los renombra, `toObject` falla con
# "No properties to serialize found". Por eso se conserva la clase completa.
-keepattributes Signature
-keep class com.example.controldegastos.model.** { *; }

# Conservar anotaciones de Firestore (@PropertyName, @Exclude, etc.).
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Mantener la información de línea para reportes de fallos.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
