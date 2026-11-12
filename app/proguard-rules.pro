# Reglas de R8/ProGuard para Control de Gastos.

# Firestore usa reflexión para convertir documentos a los modelos. Sin estas
# reglas, los campos se renombran y `toObject` devuelve objetos vacíos.
-keepattributes Signature
-keepclassmembers class com.example.controldegastos.model.** {
    <init>();
    <fields>;
}
-keepnames class com.example.controldegastos.model.**

# Conservar anotaciones de Firestore (@PropertyName, @Exclude, etc.).
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Mantener la información de línea para reportes de fallos.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
