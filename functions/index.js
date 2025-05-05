const functions = require('firebase-functions/v2');
const { GoogleSpreadsheet } = require('google-spreadsheet');
const admin = require('firebase-admin');
admin.initializeApp();

// Replace with your Google Sheet ID (the string in the URL after /d/)
const SHEET_ID = '1pueVRuzleYq-sDk5W7Fg7TuEp7UicZkYyVWsNjmBefs';

// Replace with the content of your service account key JSON file
const SERVICE_ACCOUNT_KEY = require('./serviceAccountKey.json');

exports.onNewKioskData = functions.database.onValueCreated('/kiosk_data/{pushId}',  async (event) => {
    // .onCreate(async (snapshot, context) => {
        try {
            // const newData = snapshot.val();
            const newData = event.data.val();
            const timestamp = newData.timestamp;
            const cashierPhone = newData.phone;
            const shift = newData.shift;
            const totalSales = newData.sales;
            const expenses = newData.expenses;

            // Initialize the Google Sheets document
            const doc = new GoogleSpreadsheet(SHEET_ID);

            // Authenticate with the service account
            //await doc.useServiceAccountAuth(SERVICE_ACCOUNT_KEY);
            await doc.useServiceAccountAuth({
              client_email: SERVICE_ACCOUNT_KEY.client_email,
              private_key: SERVICE_ACCOUNT_KEY.private_key.replace(/\\n/g, '\n'),
            });

            // Load sheet info (fetches document properties and all of the sheets)
            await doc.loadInfo();

            // Get the first sheet (you might want to target a specific sheet by title)
            const sheet = doc.sheetsByIndex[0];

            // Add a new row with the data
            await sheet.addRow([timestamp, cashierPhone, shift, totalSales, expenses]);

            console.log('Data successfully written to Google Sheet');
            return null;

        } catch (error) {
            console.error('Error writing to Google Sheet:', error);
            return null;
        }
    });