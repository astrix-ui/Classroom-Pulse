const { Client } = require('@stomp/stompjs');

const BASE_URL = 'http://localhost:8080/api';
const WS_URL = 'ws://localhost:8080/ws';

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const res = await fetch(url, {
    ...options,
    headers
  });
  const data = await res.json();
  return { status: res.status, ok: res.ok, data };
}

async function runTest() {
  console.log('--- 1. Testing Health Endpoint ---');
  const health = await request('/health');
  console.log('Health check:', health.data);
  if (health.data.status !== 'UP') throw new Error('Health check failed');

  console.log('\n--- 2. Creating Classroom ---');
  const createRes = await request('/classrooms', { method: 'POST' });
  console.log('Classroom created:', createRes.data);
  const { roomCode, teacherToken } = createRes.data;
  if (!roomCode || roomCode.length !== 6 || !teacherToken) {
    throw new Error('Invalid classroom creation response');
  }

  console.log('\n--- 3. Connecting STOMP WebSocket Client ---');
  const receivedEvents = [];

  const stompClient = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 0,
    debug: () => {},
  });

  await new Promise((resolve, reject) => {
    stompClient.onConnect = () => {
      console.log('STOMP client connected successfully!');
      stompClient.subscribe(`/topic/classrooms/${roomCode}`, (msg) => {
        const evt = JSON.parse(msg.body);
        console.log(`[WS EVENT RECEIVED] Type: ${evt.type}, Room: ${evt.roomCode}, Time: ${evt.timestamp}`);
        receivedEvents.push(evt);
      });
      resolve();
    };
    stompClient.onStompError = (frame) => {
      reject(new Error(`STOMP error: ${frame.headers['message']}`));
    };
    stompClient.onWebSocketError = (err) => {
      reject(err);
    };
    stompClient.activate();
  });

  // Short pause to ensure subscription is established
  await new Promise((r) => setTimeout(r, 400));

  console.log('\n--- 4. Joining 3 Students ---');
  const s1Res = await request(`/classrooms/${roomCode}/students`, {
    method: 'POST',
    body: JSON.stringify({ studentName: 'Ayush' })
  });
  console.log('Student 1 joined:', s1Res.data);

  const s2Res = await request(`/classrooms/${roomCode}/students`, {
    method: 'POST',
    body: JSON.stringify({ studentName: 'Deepa' })
  });
  console.log('Student 2 joined:', s2Res.data);

  const s3Res = await request(`/classrooms/${roomCode}/students`, {
    method: 'POST',
    body: JSON.stringify({ studentName: 'Rohan' })
  });
  console.log('Student 3 joined:', s3Res.data);

  await new Promise((r) => setTimeout(r, 400));

  console.log('\n--- 5. Publishing Question 1 ---');
  const q1Res = await request(`/classrooms/${roomCode}/questions`, {
    method: 'POST',
    headers: { 'X-Teacher-Token': teacherToken },
    body: JSON.stringify({ questionText: 'Explain the JVM architecture.' })
  });
  console.log('Question 1 published:', q1Res.data);
  const q1Id = q1Res.data.questionId;

  await new Promise((r) => setTimeout(r, 400));

  console.log('\n--- 6. Students Marking Completed ---');
  console.log('Student 1 (Ayush) marking completed...');
  const comp1 = await request(`/classrooms/${roomCode}/students/${s1Res.data.studentId}/complete`, {
    method: 'POST',
    body: JSON.stringify({ questionId: q1Id })
  });
  console.log(`Classroom progress after S1: ${comp1.data.completedCount}/${comp1.data.totalStudents} (${comp1.data.completionPercentage}%)`);

  console.log('Student 2 (Deepa) marking completed...');
  const comp2 = await request(`/classrooms/${roomCode}/students/${s2Res.data.studentId}/complete`, {
    method: 'POST',
    body: JSON.stringify({ questionId: q1Id })
  });
  console.log(`Classroom progress after S2: ${comp2.data.completedCount}/${comp2.data.totalStudents} (${comp2.data.completionPercentage}%)`);

  console.log('\n--- 7. Idempotency Test: S1 clicks COMPLETED again ---');
  const comp1Repeat = await request(`/classrooms/${roomCode}/students/${s1Res.data.studentId}/complete`, {
    method: 'POST',
    body: JSON.stringify({ questionId: q1Id })
  });
  console.log(`Classroom progress after repeated call: ${comp1Repeat.data.completedCount}/${comp1Repeat.data.totalStudents} (${comp1Repeat.data.completionPercentage}%)`);
  if (comp1Repeat.data.completedCount !== 2) {
    throw new Error('Idempotency failed: count modified on duplicate completion!');
  }

  console.log('\n--- 8. Teacher Resets Statuses ---');
  const resetRes = await request(`/classrooms/${roomCode}/reset`, {
    method: 'POST',
    headers: { 'X-Teacher-Token': teacherToken }
  });
  console.log(`Classroom progress after reset: ${resetRes.data.completedCount}/${resetRes.data.totalStudents}`);
  if (resetRes.data.completedCount !== 0) {
    throw new Error('Reset failed: completed count should be 0');
  }

  console.log('\n--- 9. Teacher Publishes Question 2 ---');
  const q2Res = await request(`/classrooms/${roomCode}/questions`, {
    method: 'POST',
    headers: { 'X-Teacher-Token': teacherToken },
    body: JSON.stringify({ questionText: 'What is the difference between ArrayList and LinkedList?' })
  });
  console.log('Question 2 published:', q2Res.data);

  console.log('\n--- 10. Stale Question Completion Test ---');
  const staleRes = await request(`/classrooms/${roomCode}/students/${s1Res.data.studentId}/complete`, {
    method: 'POST',
    body: JSON.stringify({ questionId: q1Id }) // Old question 1
  });
  console.log(`Stale question completion status: ${staleRes.status} (${staleRes.data.error}: ${staleRes.data.message})`);
  if (staleRes.status !== 409) {
    throw new Error('Stale question completion should have been rejected with 409 Conflict!');
  }

  console.log('\n--- 11. Teacher Ends Classroom Session ---');
  const closeRes = await request(`/classrooms/${roomCode}/close`, {
    method: 'POST',
    headers: { 'X-Teacher-Token': teacherToken }
  });
  console.log('Classroom closed:', closeRes.data.active === false ? 'INACTIVE' : 'ACTIVE');

  const joinAfterClose = await request(`/classrooms/${roomCode}/students`, {
    method: 'POST',
    body: JSON.stringify({ studentName: 'LateStudent' })
  });
  console.log(`Join after close status: ${joinAfterClose.status} (${joinAfterClose.data.message})`);
  if (joinAfterClose.status !== 409) {
    throw new Error('Join after close should be rejected!');
  }

  await new Promise((r) => setTimeout(r, 600));

  stompClient.deactivate();

  console.log(`\nTotal WebSocket broadcast events received: ${receivedEvents.length}`);
  const eventTypes = receivedEvents.map(e => e.type);
  console.log('Event types received:', eventTypes);

  console.log('\n--- ALL INTEGRATION TESTS PASSED SUCCESSFULLY ---');
}

runTest().catch((err) => {
  console.error('\nFAILED: ', err);
  process.exit(1);
});
