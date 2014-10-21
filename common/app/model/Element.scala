package model

import org.joda.time.Duration
import com.gu.openplatform.contentapi.model.{Element => ApiElement}

trait Element {
  def delegate: ApiElement
  def index: Int
  lazy val id: String = delegate.id

  lazy val isMain = delegate.relation == "main"
  lazy val isBody = delegate.relation == "body"
  lazy val isGallery = delegate.relation == "gallery"
  lazy val isThumbnail = delegate.relation == "thumbnail"
}

object Element {
  def apply(theDelegate: ApiElement, elementIndex: Int): Element = {
    theDelegate.elementType match {
      case "image" => ImageElement(theDelegate, elementIndex)
      case "video" => VideoElement(theDelegate, elementIndex)
      case "audio" => AudioElement(theDelegate, elementIndex)
      case "embed" => EmbedElement(theDelegate, elementIndex)
      case _ => new Element{
        lazy val delegate = theDelegate
        lazy val index = elementIndex
      }
    }
  }
}

object ImageContainerTypeclass {
  trait ImageContainer {
    val imageCrops: Seq[ImageAsset]
    val largestImage: Option[ImageAsset]
    val largestEditorialCrop: Option[ImageAsset]
  }
  object ImageContainer {
    implicit class implicitForElement(element: Element) extends ImageContainer {
      lazy val imageCrops: Seq[ImageAsset] =
        element.delegate.assets.filter(_.assetType == "image").map(ImageAsset(_,element.index)).sortBy(-_.width)
      lazy val largestImage: Option[ImageAsset] = imageCrops.headOption
      lazy val largestEditorialCrop: Option[ImageAsset] = imageCrops.find(img => img.width < img.height || (img.width != 2048 && img.width != 1024))
    }
    implicit class implicitForImageElement(imageElement: ImageElement) extends ImageContainer {
      lazy val imageCrops: Seq[ImageAsset] =
        imageElement.crops.getOrElse(imageElement.delegate.assets.filter(_.assetType == "image").map(ImageAsset(_,imageElement.index)).sortBy(-_.width))
      lazy val largestImage: Option[ImageAsset] = imageCrops.headOption
      lazy val largestEditorialCrop: Option[ImageAsset] = imageCrops.find(img => img.width < img.height || (img.width != 2048 && img.width != 1024))
    }
  }
}

case class ImageElement(delegate: ApiElement, index: Int, crops: Option[Seq[ImageAsset]] = None) extends Element {

  lazy val imageCrops: Seq[ImageAsset] =
    crops.getOrElse(delegate.assets.filter(_.assetType == "image").map(ImageAsset(_,index)).sortBy(-_.width))


  // The image crop with the largest width.
  lazy val largestImage: Option[ImageAsset] = imageCrops.headOption

  // all landscape images with a width of 1024 or 2048 have been auto-cropped to 4:3. portrait images are never
  // auto-cropped.. this is a temporary solution until the new media service is in use and we can properly
  // distinguish crops by their intended usage
  lazy val largestEditorialCrop: Option[ImageAsset] = imageCrops.find(img => img.width < img.height || (img.width != 2048 && img.width != 1024))
}

object ImageElement {
  def apply(crops: Seq[ImageAsset], theDelegate: ApiElement, imageIndex: Int): ImageElement =
    ImageElement(theDelegate, imageIndex, Option(crops))
}

case class VideoElement(delegate: ApiElement, index: Int) extends Element {

  protected implicit val ordering = EncodingOrdering

  lazy val videoAssets: List[VideoAsset] = {
    val images = delegate.assets.filter(_.assetType == "image").zipWithIndex.map{ case (asset, index) =>
      ImageAsset(asset, index)
    }

    val container = images.headOption.map(img => ImageElement(images, delegate, img.index))

    delegate.assets.filter(_.assetType == "video").map( v => VideoAsset(v, container)).sortBy(-_.width)
  }

  lazy val blockVideoAds = videoAssets.exists(_.blockVideoAds)

  lazy val encodings: Seq[Encoding] = {
    videoAssets.toList.collect {
      case video: VideoAsset => video.encoding
    }.flatten.sorted
  }
  lazy val duration: Int = videoAssets.headOption.map(_.duration).getOrElse(0)
  lazy val ISOduration: String = new Duration(duration*1000.toLong).toString()
  lazy val height: String = videoAssets.headOption.map(_.height).getOrElse(0).toString
  lazy val width: String = videoAssets.headOption.map(_.width).getOrElse(0).toString

  lazy val largestVideo: Option[VideoAsset] = videoAssets.headOption

  lazy val source: Option[String] = videoAssets.headOption.flatMap(_.source)
  lazy val caption: Option[String] = largestVideo.flatMap(_.caption)
}

case class AudioElement(delegate: ApiElement, index: Int) extends Element {
  protected implicit val ordering = EncodingOrdering
  lazy val audioAssets: List[AudioAsset] = delegate.assets.filter(_.assetType == "audio").map( v => AudioAsset(v))
  lazy val duration: Int = audioAssets.headOption.map(_.duration).getOrElse(0)
  lazy val encodings: Seq[Encoding] = {
    audioAssets.toList.collect {
      case audio: AudioAsset => Encoding(audio.url.getOrElse(""), audio.mimeType.getOrElse(""))
    }.sorted
  }
}

case class EmbedElement(delegate: ApiElement, index: Int) extends Element {
   lazy val embedAssets: Seq[EmbedAsset] = delegate.assets.filter(_.assetType == "embed").map(EmbedAsset(_))
}