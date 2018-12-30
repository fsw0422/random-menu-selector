package src.utils.emailer

case class AttachmentDescription(content: Array[Byte],
                                 filename: String,
                                 contentType: String,
                                 description: String)
