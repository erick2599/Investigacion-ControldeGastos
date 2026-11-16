import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { afterAll, afterEach, beforeAll, describe, it } from "vitest";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";

const here = dirname(fileURLToPath(import.meta.url));
const firestoreRules = readFileSync(resolve(here, "../firestore.rules"), "utf8");
const projectId = "demo-controldegastos";

const UID_A = "uid-alice";
const UID_B = "uid-bob";
const EMAIL_LEGACY_MIXED = "Usuario.Legacy@Example.com";
/** Token de Firebase Auth con el correo ya verificado. */
const VERIFICADO = { email_verified: true };
const SIN_VERIFICAR = { email_verified: false };

let testEnv;

const perfilValido = (nombre = "Alice") => ({ nombre });

const gastoValido = (extra = {}) => ({
  titulo: "Supermercado",
  monto: 42.5,
  categoriaLaboral: "Hogar",
  fecha: "12/11/2026",
  fechaMillis: 1762905600000,
  ...extra,
});

const ingresoValido = (extra = {}) => ({
  concepto: "Salario",
  monto: 1500,
  categoria: "Trabajo",
  fecha: "01/11/2026",
  fechaMillis: 1761955200000,
  ...extra,
});

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: firestoreRules,
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

afterAll(async () => {
  await testEnv.cleanup();
});

describe("perfil usuarios/{uid}", () => {
  it("permite al propietario leer y escribir su perfil", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    await assertSucceeds(db.doc(`usuarios/${UID_A}`).set(perfilValido()));
    await assertSucceeds(db.doc(`usuarios/${UID_A}`).get());
  });

  it("rechaza a un usuario autenticado leer el perfil de otro", async () => {
    const db = testEnv.authenticatedContext(UID_B, VERIFICADO).firestore();
    await assertFails(db.doc(`usuarios/${UID_A}`).get());
  });

  it("rechaza escribir el perfil de otro", async () => {
    const db = testEnv.authenticatedContext(UID_B, VERIFICADO).firestore();
    await assertFails(db.doc(`usuarios/${UID_A}`).set(perfilValido("Mallory")));
  });

  it("rechaza el acceso sin autenticación", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(db.doc(`usuarios/${UID_A}`).get());
    await assertFails(db.doc(`usuarios/${UID_A}`).set(perfilValido()));
  });

  it("permite migrar desde la ruta antigua usuarios/{email}", async () => {
    const db = testEnv
      .authenticatedContext("uid-nuevo", {
        email: EMAIL_LEGACY_MIXED,
        email_verified: true,
      })
      .firestore();
    await assertSucceeds(db.doc(`usuarios/${EMAIL_LEGACY_MIXED}`).get());
    await assertSucceeds(
      db.doc(`usuarios/${EMAIL_LEGACY_MIXED}`).set(perfilValido("Legacy"))
    );
    // La variante en minúsculas también debe funcionar (email escrito distinto).
    await assertSucceeds(
      db.doc(`usuarios/${EMAIL_LEGACY_MIXED.toLowerCase()}`).get()
    );
  });

  it("no permite a otro usuario acceder a una ruta con forma de email", async () => {
    const db = testEnv
      .authenticatedContext(UID_B, { email: "otro@example.com", email_verified: true })
      .firestore();
    await assertFails(db.doc(`usuarios/${EMAIL_LEGACY_MIXED}`).get());
  });
});

describe("subcolección gastos", () => {
  it("permite crear, leer, actualizar y borrar al propietario", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);

    await assertSucceeds(gasto.set(gastoValido()));
    await assertSucceeds(gasto.get());
    await assertSucceeds(gasto.update({ monto: 50 }));
    await assertSucceeds(gasto.delete());
  });

  it("rechaza operaciones de otro usuario", async () => {
    const db = testEnv.authenticatedContext(UID_B, VERIFICADO).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);

    await assertFails(gasto.set(gastoValido()));
    await assertFails(gasto.get());
    await assertFails(gasto.delete());
  });

  it("rechaza montos no válidos", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);

    await assertFails(gasto.set(gastoValido({ monto: 0 })));
    await assertFails(gasto.set(gastoValido({ monto: -10 })));
    await assertFails(gasto.set(gastoValido({ monto: "100" })));
    await assertFails(gasto.set(gastoValido({ monto: 1_000_000_001 })));
  });

  it("rechaza títulos vacíos, demasiado largos o de tipo incorrecto", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);

    await assertFails(gasto.set(gastoValido({ titulo: "" })));
    await assertFails(gasto.set(gastoValido({ titulo: "x".repeat(121) })));
    await assertFails(gasto.set(gastoValido({ titulo: 123 })));
  });

  it("rechaza documentos a los que les falta un campo obligatorio", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);
    const { categoriaLaboral: _, ...sinCategoria } = gastoValido();

    await assertFails(gasto.set(sinCategoria));
  });
});

describe("subcolección ingresos", () => {
  it("permite crear y leer al propietario", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const ingreso = db.doc(`usuarios/${UID_A}/ingresos/i1`);

    await assertSucceeds(ingreso.set(ingresoValido()));
    await assertSucceeds(ingreso.get());
  });

  it("rechaza a otro usuario", async () => {
    const db = testEnv.authenticatedContext(UID_B, VERIFICADO).firestore();
    await assertFails(db.doc(`usuarios/${UID_A}/ingresos/i1`).set(ingresoValido()));
  });

  it("rechaza conceptos vacíos y montos no válidos", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const ingreso = db.doc(`usuarios/${UID_A}/ingresos/i1`);

    await assertFails(ingreso.set(ingresoValido({ concepto: "" })));
    await assertFails(ingreso.set(ingresoValido({ monto: 0 })));
  });
});

describe("correo sin verificar", () => {
  it("permite crear el perfil durante el registro", async () => {
    const db = testEnv.authenticatedContext(UID_A, SIN_VERIFICAR).firestore();

    await assertSucceeds(db.doc(`usuarios/${UID_A}`).set(perfilValido()));
  });

  it("no permite leer ni modificar el perfil", async () => {
    const db = testEnv.authenticatedContext(UID_A, SIN_VERIFICAR).firestore();

    await assertSucceeds(db.doc(`usuarios/${UID_A}`).set(perfilValido()));
    await assertFails(db.doc(`usuarios/${UID_A}`).get());
    await assertFails(db.doc(`usuarios/${UID_A}`).set(perfilValido("Otro")));
  });

  it("no permite leer ni escribir gastos e ingresos", async () => {
    const db = testEnv.authenticatedContext(UID_A, SIN_VERIFICAR).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);
    const ingreso = db.doc(`usuarios/${UID_A}/ingresos/i1`);

    await assertFails(gasto.set(gastoValido()));
    await assertFails(gasto.get());
    await assertFails(ingreso.set(ingresoValido()));
    await assertFails(ingreso.get());
  });

  it("permite los datos en cuanto el token trae email_verified", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    const gasto = db.doc(`usuarios/${UID_A}/gastos/g1`);

    await assertSucceeds(gasto.set(gastoValido()));
    await assertSucceeds(gasto.get());
  });
});

describe("cierre por defecto", () => {
  it("rechaza colecciones no declaradas aunque el usuario esté autenticado", async () => {
    const db = testEnv.authenticatedContext(UID_A, VERIFICADO).firestore();
    await assertFails(db.doc("publico/dato").get());
    await assertFails(db.doc("publico/dato").set({ x: 1 }));
  });
});
