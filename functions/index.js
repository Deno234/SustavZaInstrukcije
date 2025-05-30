const {onValueCreated} = require("firebase-functions/v2/database");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

const firestore = getFirestore();

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

      // 1. Nemoj slati notifikaciju pošiljatelju
      if (senderId === receiverId) {
        console.log("Sender is the same as receiver, no notification sent.");
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
          notification: {
            title: `Nova poruka od ${senderName}`,
            body: messageText.length > 100 ?
                    messageText.substring(0, 97) + "..." : messageText,
          },
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
