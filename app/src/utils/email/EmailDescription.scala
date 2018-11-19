package src.utils.email

case class EmailDescription(emails: Array[String],
                            subject: String,
                            message: String,
                            replyToEmails: Array[String],
                            ccEmails: Array[String],
                            bccEmails: Array[String])
