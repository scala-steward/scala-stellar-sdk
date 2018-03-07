package stellar.sdk

import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats, JValue}

case class OrderBook(selling: Asset, buying: Asset, bids: Seq[Order], asks: Seq[Order]) {
  def base = selling

  def counter = buying
}

case class Order(price: Price, quantity: Long)

object OrderBookDeserializer extends CustomSerializer[OrderBook](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    def asset(obj: JValue) = {
      def assetCode = (obj \ s"asset_code").extract[String]

      def assetIssuer = KeyPair.fromAccountId((obj \ s"asset_issuer").extract[String])

      (obj \ s"asset_type").extract[String] match {
        case "native" => NativeAsset
        case "credit_alphanum4" => AssetTypeCreditAlphaNum4(assetCode, assetIssuer)
        case "credit_alphanum12" => AssetTypeCreditAlphaNum12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }

    def orders(obj: JValue) = {
      obj.children.map(c =>
        Order(
          price = Price(
            n = (c \ "price_r" \ "n").extract[Int],
            d = (c \ "price_r" \ "d").extract[Int]
          ),
          quantity = Amount.toBaseUnits((c \ "amount").extract[String].toDouble).get
        ))
    }

    try {
      OrderBook(
        selling = asset(o \ "base"),
        buying = asset(o \ "counter"),
        bids = orders(o \ "bids"),
        asks = orders(o \ "asks")
      )
    } catch {
      case t: Throwable => throw new RuntimeException(pretty(render(o)), t)
    }
}, PartialFunction.empty)
)