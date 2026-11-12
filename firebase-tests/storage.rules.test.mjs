import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { afterAll, afterEach, beforeAll, describe, it } from "vitest";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { deleteObject, getBytes, ref, uploadBytes } from "firebase/storage";

const here = dirname(fileURLToPath(import.meta.url));
const storageRules = readFileSync(resolve(here, "../storage.rules"), "utf8");
const projectId = "demo-controldegastos";

const UID_A = "uid-alice";
const UID_B = "uid-bob";
const PATH_A = `usuarios/${UID_A}/recibos/recibo.jpg`;
const PATH_B = `usuarios/${UID_B}/recibos/recibo.jpg`;

const imagen = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46]);

let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    storage: {
      rules: storageRules,
      host: "127.0.0.1",
      port: 9199,
    },
  });
});

afterEach(async () => {
  await testEnv.clearStorage();
});

afterAll(async () => {
  await testEnv.cleanup();
});

describe("recibos en Storage", () => {
  it("permite al propietario subir, leer y borrar una imagen", async () => {
    const storage = testEnv.authenticatedContext(UID_A).storage();

    await assertSucceeds(
      uploadBytes(ref(storage, PATH_A), imagen, { contentType: "image/jpeg" })
    );
    await assertSucceeds(getBytes(ref(storage, PATH_A)));
    await assertSucceeds(deleteObject(ref(storage, PATH_A)));
  });

  it("rechaza subir a la carpeta de otro usuario", async () => {
    const storage = testEnv.authenticatedContext(UID_B).storage();

    await assertFails(
      uploadBytes(ref(storage, PATH_A), imagen, { contentType: "image/jpeg" })
    );
  });

  it("rechaza leer o borrar recibos de otro usuario", async () => {
    const storageA = testEnv.authenticatedContext(UID_A).storage();
    await uploadBytes(ref(storageA, PATH_A), imagen, { contentType: "image/jpeg" });

    const storageB = testEnv.authenticatedContext(UID_B).storage();
    await assertFails(getBytes(ref(storageB, PATH_A)));
    await assertFails(deleteObject(ref(storageB, PATH_A)));
  });

  it("rechaza el acceso sin autenticación", async () => {
    const storage = testEnv.unauthenticatedContext().storage();

    await assertFails(
      uploadBytes(ref(storage, PATH_A), imagen, { contentType: "image/jpeg" })
    );
    await assertFails(getBytes(ref(storage, PATH_A)));
  });

  it("rechaza archivos que no son imágenes", async () => {
    const storage = testEnv.authenticatedContext(UID_A).storage();

    await assertFails(
      uploadBytes(ref(storage, PATH_A), new TextEncoder().encode("no soy imagen"), {
        contentType: "text/plain",
      })
    );
  });

  it("rechaza imágenes de 5 MB o más", async () => {
    const storage = testEnv.authenticatedContext(UID_A).storage();
    const grande = new Uint8Array(5 * 1024 * 1024);

    await assertFails(
      uploadBytes(ref(storage, PATH_A), grande, { contentType: "image/jpeg" })
    );
  });

  it("rechaza rutas fuera de usuarios/{uid}/recibos", async () => {
    const storage = testEnv.authenticatedContext(UID_A).storage();

    await assertFails(
      uploadBytes(ref(storage, `usuarios/${UID_A}/otra/recibo.jpg`), imagen, {
        contentType: "image/jpeg",
      })
    );
  });
});
