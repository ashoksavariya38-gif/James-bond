const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_study_123";
const BOB_UID = "bob_study_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated users cannot read or write any study collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcardDecks")
      .get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savedCourses").get()
  );

  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .set({
        userId: ALICE_UID,
        title: "Unauthorized Session",
        preview: "",
        modelMode: "GEMINI_FAST",
        languageCode: "en",
        createdAt: past,
        updatedAt: past,
      })
  );
});

test("Authenticated owner: valid StudySession and ChatMessage create, partial update, and query succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("sess_1");

  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "Calculus Derivatives",
      preview: "Explain chain rule step by step",
      modelMode: "GEMINI_FAST",
      languageCode: "en",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    sessionRef.update({
      preview: "Step 1: Identify outer and inner functions...",
      modelMode: "GEMINI_DEEP",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .orderBy("updatedAt", "desc")
      .get()
  );

  const msgRef = sessionRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      sessionId: "sess_1",
      role: "assistant",
      content: "The chain rule states...",
      modelLabel: "Gemini Fast",
      sourceUrls: ["https://example.edu/calculus"],
      createdAt: past,
    })
  );

  await assertSucceeds(
    msgRef.update({
      secondaryContent: "ChatGPT second opinion: Think of nested functions.",
      secondaryModelLabel: "ChatGPT",
    })
  );

  await assertSucceeds(
    sessionRef.collection("messages").orderBy("createdAt", "asc").get()
  );
});

test("Rejects @DocumentId 'id' injection, ghost fields, future timestamps, and immutable createdAt mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const future = new Date(Date.now() + 60000);

  // Reject 'id' field in payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc("note_bad_id")
      .set({
        id: "note_bad_id",
        userId: ALICE_UID,
        title: "Bad Note",
        content: "Body",
        languageCode: "en",
        tags: [],
        createdAt: past,
        updatedAt: past,
      })
  );

  // Reject ghost field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc("note_ghost")
      .set({
        userId: ALICE_UID,
        title: "Ghost",
        content: "Body",
        languageCode: "en",
        tags: [],
        isAdmin: true,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc("note_future")
      .set({
        userId: ALICE_UID,
        title: "Future",
        content: "Body",
        languageCode: "en",
        tags: [],
        createdAt: future,
        updatedAt: future,
      })
  );

  // Create valid note then attempt to mutate createdAt
  const noteRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("notes")
    .doc("note_1");
  await assertSucceeds(
    noteRef.set({
      userId: ALICE_UID,
      title: "Valid Note",
      content: "Thermodynamics laws",
      languageCode: "es",
      tags: ["physics"],
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    noteRef.update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("FlashcardDeck, SavedCourse, and StudyStats succeed for owner and block cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcardDecks")
      .doc("deck_1")
      .set({
        userId: ALICE_UID,
        title: "French Verbs",
        topic: "Irregular conjugation",
        languageCode: "fr",
        cards: [
          { id: "c1", front: "Être", back: "To be", status: "NEW" },
          { id: "c2", front: "Avoir", back: "To have", status: "KNOW_IT" },
        ],
        createdAt: past,
        updatedAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedCourses")
      .doc("course_1")
      .set({
        userId: ALICE_UID,
        title: "CS50's Introduction to Computer Science",
        provider: "HarvardX",
        language: "English",
        level: "Beginner",
        duration: "12 weeks",
        cost: "Free",
        url: "https://cs50.harvard.edu",
        summary: "Foundational CS course",
        createdAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profile")
      .doc("stats")
      .set({
        userId: ALICE_UID,
        completedPomodoros: 2,
        totalFocusMinutes: 50,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Cross-user isolation checks
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcardDecks")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savedCourses")
      .doc("course_1")
      .delete()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profile")
      .doc("stats")
      .update({
        completedPomodoros: 99,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});
