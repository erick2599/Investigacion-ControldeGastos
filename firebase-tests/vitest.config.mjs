import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["*.test.mjs"],
    // Los emuladores son un recurso compartido: ejecutar los archivos en serie
    // evita condiciones de carrera al limpiar los datos entre pruebas.
    fileParallelism: false,
    testTimeout: 30_000,
    hookTimeout: 30_000,
    reporters: ["verbose"],
  },
});
