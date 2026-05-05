const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendChatPushOnNewMessage = onDocumentCreated(
  {
    document: "conversations/{conversationId}/messages/{messageId}",
    region: "asia-southeast1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const msg = snap.data() || {};
    const senderId = msg.senderId || "";
    const receiverId = msg.receiverId || "";
    const type = msg.type || "text";
    const text = msg.text || "";

    if (!senderId || !receiverId || senderId === receiverId) return;

    const db = admin.firestore();
    const [receiverDoc, senderDoc] = await Promise.all([
      db.collection("users").doc(receiverId).get(),
      db.collection("users").doc(senderId).get(),
    ]);

    if (!receiverDoc.exists) {
      logger.warn("Receiver document missing", { receiverId });
      return;
    }

    const receiver = receiverDoc.data() || {};
    const sender = senderDoc.exists ? senderDoc.data() || {} : {};
    const fcmToken = receiver.fcmToken || "";
    if (!fcmToken) {
      logger.info("Receiver has no fcmToken", { receiverId });
      return;
    }

    const senderName = sender.fullName || sender.nickname || "Chef";
    const senderAvatar = sender.avatarUrl || "";
    const body =
      type === "image"
        ? "Sent a photo"
        : type === "video"
          ? "Sent a video"
          : type === "voice"
            ? "Sent a voice message"
            : type === "attachment"
              ? "Sent an attachment"
              : (text || "Sent a message");

    const message = {
      token: fcmToken,
      data: {
        title: senderName,
        body,
        otherUid: senderId,
        otherName: senderName,
        otherAvatar: senderAvatar,
      },
      android: {
        priority: "high",
      },
    };

    await admin.messaging().send(message);
    logger.info("Chat push sent", {
      receiverId,
      senderId,
      messageId: event.params.messageId,
    });
  }
);
