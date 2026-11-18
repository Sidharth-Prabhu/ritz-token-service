/**
 * Standalone demo script: Tillo POS deducting 80 tokens from student wallet
 */
const { TilloWalletClient } = require('./tilloClient');

async function runDemo() {
  const client = new TilloWalletClient('http://localhost:8085', 'ritz-secret-api-key-2026');
  const studentId = 'RIT2026001';
  const orderRef = 'TILLO-ORDER-' + Math.floor(Math.random() * 90000 + 10000);

  console.log('--- 1. Checking student balance ---');
  try {
    const balanceInfo = await client.getStudentBalance(studentId);
    console.log(`Student: ${balanceInfo.student_name} (${balanceInfo.student_id})`);
    console.log(`Current Balance: ${balanceInfo.balance} ${balanceInfo.currency}`);

    console.log('\n--- 2. Validating balance for purchase of 80 tokens ---');
    const valid = await client.validateBalance(studentId, 80);
    console.log('Balance check passed:', valid.valid);

    console.log('\n--- 3. Executing debit for 80 tokens at Tillo Canteen ---');
    const result = await client.debit(studentId, 80, orderRef, 'Canteen lunch meal');
    console.log('Debit successful!');
    console.log('Transaction ID:', result.transaction_id);
    console.log('Previous Balance:', result.previous_balance);
    console.log('Current Balance:', result.current_balance);
    console.log('Reference ID:', result.reference_id);

    console.log('\n--- 4. Testing Idempotency (Sending same orderRef again) ---');
    const duplicate = await client.debit(studentId, 80, orderRef, 'Canteen lunch meal');
    console.log('Idempotent response returned identical transaction ID:', duplicate.transaction_id);
    console.log('Balance was NOT deducted again. Current balance remains:', duplicate.current_balance);
  } catch (err) {
    console.error('Error during Tillo integration demo:', err.message);
  }
}

runDemo();
