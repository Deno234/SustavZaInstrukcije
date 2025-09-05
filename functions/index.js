const {onValueCreated} = require("firebase-functions/v2/database");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

initializeApp();

const firestore = getFirestore();

const lastMessageTimestamps = new Map();

exports.sendNewMessageNotification = onValueCreated(
    {
      ref: "/chats/{chatId}/messages/{messageId}",
      region: "europe-west1",
    },
    async (event) => {
      const snapshot = event.data;
      const messageData = snapshot.val();
      const chatId = event.params.chatId;

      const senderId = messageData.senderId;
      const receiverId = messageData.receiverId;
      const messageText = messageData.text;
      const messageTimestamp = messageData.timestamp;

      // 1. Nemoj slati notifikaciju pošiljatelju
      if (senderId === receiverId) {
        console.log("Sender is the same as receiver, no notification sent.");
        return null;
      }

      const lastTimestamp = lastMessageTimestamps.get(senderId) || 0;
      if (messageTimestamp <= lastTimestamp) {
        return null;
      }

      // Dodaj kratki delay da se osigura da su sve poruke stigle
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Provjeri je li ova poruka još uvijek najnovija
      const currentLatestTimestamp = lastMessageTimestamps.get(senderId);
      if (messageTimestamp < currentLatestTimestamp) {
        return null;
      }

      // 3. Dohvati FCM token(e)
      let recipientTokens = [];
      try {
        const userDoc = await firestore
            .collection("users").doc(receiverId).get();
        if (userDoc.exists) {
          const userData = userDoc.data();
          if (userData.fcmToken) {
            recipientTokens.push(userData.fcmToken);
          } else if (userData.fcmTokens && Array.isArray(userData.fcmTokens)) {
            recipientTokens = userData.fcmTokens;
          }
        }
        if (recipientTokens.length === 0) {
          console.log("Receiver FCM token(s) not found for user:", receiverId);
          return null;
        }
      } catch (error) {
        console.error("Error fetching recipient token(s):", error);
        return null;
      }

      // 4. Dohvati ime pošiljatelja
      let senderName = "Netko";
      try {
        const senderDoc = await firestore
            .collection("users").doc(senderId).get();
        if (senderDoc.exists && senderDoc.data().name) {
          senderName = senderDoc.data().name;
        }
      } catch (error) {
        console.error("Error fetching sender name:", error);
      }

      // 6. Pošalji notifikaciju
      try {
        const messaging = getMessaging();

        // Kreiraj poruke za svaki token
        const messages = recipientTokens.map((token) => ({
          data: {
            chatId: chatId,
            otherUserId: senderId,
            senderName: senderName,
            title: `Nova poruka od ${senderName}`,
            body: messageText,
            navigateTo: "ChatScreen",
          },
          token: token,
        }));

        // Pošalji sve poruke odjednom
        const response = await messaging.sendEach(messages);

        console.log(
            "Successfully sent message notification(s):",
            response.successCount, "successes,",
            response.failureCount, "failures.",
        );

        // Obrada neuspješnih slanja
        const tokensToRemove = [];
        response.responses.forEach((result, index) => {
          if (!result.success) {
            const error = result.error;
            console.error(
                "Failure sending notification to token:",
                recipientTokens[index],
                error,
            );

            if ( error &&
                (error.code === "messaging/invalid-registration-token" ||
                error.code === "messaging/registration-token-not-registered")
            ) {
              tokensToRemove.push(recipientTokens[index]);
            }
          }
        });

        // Ukloni nevažeće tokene
        if (tokensToRemove.length > 0) {
          const userRef = firestore.collection("users").doc(receiverId);
          // UKLONI OVU LINIJU - FieldValue je već importiran na vrhu
          // const {FieldValue} = require("firebase-admin/firestore");

          await userRef.update({
            fcmTokens: FieldValue.arrayRemove(...tokensToRemove),
          });
          console.log("Removed invalid tokens:", tokensToRemove);
        }
      } catch (error) {
        console.error("Error sending message notification(s):", error);
      }

      return null;
    },
);

exports.sendSessionInvitation = onDocumentCreated(
    {document: "invitations/{invitationId}", region: "europe-west1"},
    async (event) => {
      const invitation = event.data.data();
      const studentIds = Array.isArray(invitation.studentIds) ?
        invitation.studentIds :
        [invitation.studentId]; // fallback na staru logiku

      const instrDoc = await firestore.collection("users")
          .doc(invitation.instructorId).get();
      const instructorName = instrDoc.exists && instrDoc.data().name ?
        instrDoc.data().name : "Instruktor";

      const messaging = getMessaging();

      for (const studentId of studentIds) {
        const studentDoc = await firestore.collection("users")
            .doc(studentId).get();
        if (!studentDoc.exists) continue;

        let recipientTokens = [];
        const userData = studentDoc.data();
        if (userData.fcmToken) {
          recipientTokens.push(userData.fcmToken);
        } else if (userData.fcmTokens && Array.isArray(userData.fcmTokens)) {
          recipientTokens = userData.fcmTokens;
        }
        if (recipientTokens.length === 0) continue;

        const messages = recipientTokens.map((token) => ({
          token,
          data: {
            type: "session_invitation",
            navigateTo: "StudentInvitations",
            sessionId: invitation.sessionId,
            instructorId: invitation.instructorId,
            subject: invitation.subject,
          },
          notification: {
            title: "Pozivnica za sjednicu",
            body: `${instructorName} vas poziva na ${invitation.subject}`,
          },
          android: {priority: "high"},
        }));

        await messaging.sendEach(messages);
      }
      return null;
    },
);


