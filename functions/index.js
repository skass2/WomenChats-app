const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.sendNotificationOnNewMessage = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    logger.info("Bắt đầu xử lý tin nhắn mới...");

    const snapshot = event.data;
    if (!snapshot) { return; }
    const messageData = snapshot.data();
    
    const senderId = messageData.senderId;
    const chatData = (await db.collection("chats").doc(event.params.chatId).get()).data();
    const receiverId = chatData.participants.find((p) => p !== senderId);

    if (!receiverId) { return; }

    const senderUserDoc = await db.collection("users").doc(senderId).get();
    const senderName = senderUserDoc.exists ? senderUserDoc.data().name : "Một người bạn";
    const senderAvatar = senderUserDoc.exists ? senderUserDoc.data().avatarUrl : "";

    const receiverUserDoc = await db.collection("users").doc(receiverId).get();
    if (!receiverUserDoc.exists || !receiverUserDoc.data().fcmToken) {
        logger.warn(`Không tìm thấy token của người nhận: ${receiverId}`);
        return;
    }
    const token = receiverUserDoc.data().fcmToken;

    // --- PAYLOAD CUỐI CÙNG: 100% LÀ DATA ---
    const payload = {
        // Chỉ có trường "data", không có "notification" hay "android.notification"
        data: {
            title: senderName,
            body: messageData.text || "Đã gửi một tệp đính kèm.",
            avatarUrl: senderAvatar || "",
            senderUid: senderId, 
        },
        android: {
            priority: "high", // Vẫn giữ ưu tiên cao để Android xử lý ngay
        },
        token: token,
    };

    try {
        await admin.messaging().send(payload);
        logger.info("Gửi thông báo DATA-ONLY thành công!");
    } catch (error) {
        logger.error("Lỗi khi gửi thông báo:", error);
    }
});