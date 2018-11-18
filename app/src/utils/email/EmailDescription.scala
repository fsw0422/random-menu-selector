package src.utils.email

case class EmailDescription(emails: Array[String],
                            message: String,
                            subject: String,
                            replyToEmails: Array[String],
                            ccEmails: Array[String],
                            bccEmails: Array[String])
