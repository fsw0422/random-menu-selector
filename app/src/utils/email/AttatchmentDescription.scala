package src.utils.email

case class AttachmentDescription(content: Array[Byte],
                                 filename: String,
                                 contentType: String,
                                 description: String)
