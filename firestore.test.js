const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "tailor_alice_123";
const BOB_UID = "tailor_bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validMeasurement(unit = "in") {
  return {
    unit,
    kameezLength: "40.5",
    shoulder: "17.5",
    chest: "42",
    kameezWaist: "38",
    kameezHip: "43",
    sleeveLength: "24.5",
    armhole: "9.5",
    neck: "15.5",
    cuff: "9",
    frontLength: "39.5",
    backLength: "40.5",
    collar: "15.5",
    shalwarLength: "41",
    shalwarWaist: "36",
    shalwarHip: "44",
    thigh: "26",
    knee: "18",
    bottomPaicha: "15",
    shalwarStyle: "Simple Shalwar",
  };
}

function validCustomerPayload(userId, nowDate = new Date(Date.now() - 5000)) {
  return {
    userId,
    customerCode: "TB-1001",
    fullName: "Muhammad Usman",
    mobileNumber: "+923001234567",
    address: "Gulberg III, Lahore",
    measurementDate: "2026-11-26",
    notes: "Single cuff, round daman",
    currentMeasurement: validMeasurement("in"),
    createdAt: nowDate,
    updatedAt: nowDate,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write shop profiles, customers, or history", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set(
      validCustomerPayload(ALICE_UID)
    )
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").collection("history").get()
  );
});

test("Owner can create and update shop profile, but cannot mutate createdAt or add ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    userRef.set({
      userId: ALICE_UID,
      shopName: "Royal Tailor House",
      shopPhone: "+923001112233",
      shopAddress: "Main Bazaar",
      preferredUnit: "in",
      preferredLanguage: "BOTH",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    userRef.update({
      shopName: "Royal Bespoke Tailors",
      preferredUnit: "cm",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    userRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    userRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create customer + history in batch, list, update measurements, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  const histRef = custRef.collection("history").doc("hist_1");

  // Batch create customer and initial history snapshot (existsAfter passes)
  const batch = aliceDb.batch();
  batch.set(custRef, validCustomerPayload(ALICE_UID, created));
  batch.set(histRef, {
    userId: ALICE_UID,
    customerId: "cust_1",
    measurementDate: "2026-11-26",
    notes: "Initial measurement",
    measurement: validMeasurement("in"),
    createdAt: created,
  });
  await assertSucceeds(batch.commit());

  // List customers & history (matches Kotlin queries)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").get()
  );
  await assertSucceeds(
    custRef.collection("history").get()
  );

  // Partial update measurements + date
  await assertSucceeds(
    custRef.update({
      measurementDate: "2026-11-27",
      notes: "Adjusted chest +0.5",
      currentMeasurement: validMeasurement("cm"),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // History entries are immutable (cannot update)
  await assertFails(
    histRef.update({
      notes: "Tampered history",
    })
  );

  // Reject @DocumentId 'id' leakage
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_2").set({
      id: "cust_2",
      ...validCustomerPayload(ALICE_UID, created),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_3").set(
      validCustomerPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Reject oversized fullName
  await assertFails(
    custRef.update({
      fullName: "A".repeat(120),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete allowed for owner
  await assertSucceeds(histRef.delete());
  await assertSucceeds(custRef.delete());
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's records", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("customers").doc("alice_cust").set(
      validCustomerPayload(ALICE_UID)
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("alice_cust").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("alice_cust").update({
      fullName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("alice_cust").delete());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("bob_spoof").set(
      validCustomerPayload(BOB_UID)
    )
  );
});
