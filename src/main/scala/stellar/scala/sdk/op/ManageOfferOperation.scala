package stellar.scala.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{ManageOfferOp, Price => XDRPrice}
import stellar.scala.sdk._

import scala.util.{Failure, Success, Try}

sealed trait ManageOfferOperation extends Operation {
  val offerId: Long = 0
}

case class CreateOfferOperation(selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[KeyPair] = None) extends ManageOfferOperation {

  override def toOperationBody: OperationBody = {
    val op = new ManageOfferOp
    op.setOfferID(uint64(0))
    op.setSelling(selling.asset.toXDR)
    op.setBuying(buying.toXDR)
    op.setAmount(int64(selling.units))
    op.setPrice(price.toXDR)
    val body = new OperationBody
    body.setDiscriminant(MANAGE_OFFER)
    body.setManageOfferOp(op)
    body
  }

}

case class DeleteOfferOperation(override val offerId: Long,
                                selling: Asset, buying: Asset, price: Price,
                                sourceAccount: Option[KeyPair] = None) extends ManageOfferOperation {

  override def toOperationBody: OperationBody = {
    val op = new ManageOfferOp
    op.setOfferID(uint64(offerId))
    op.setAmount(int64(0))
    op.setSelling(selling.toXDR)
    op.setBuying(buying.toXDR)
    op.setPrice(price.toXDR)
    val body = new OperationBody
    body.setDiscriminant(MANAGE_OFFER)
    body.setManageOfferOp(op)
    body
  }
}

case class UpdateOfferOperation(override val offerId: Long,
                                selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[KeyPair] = None) extends ManageOfferOperation {
  override def toOperationBody: OperationBody = {
    val op = new ManageOfferOp
    op.setOfferID(uint64(offerId))
    op.setSelling(selling.asset.toXDR)
    op.setBuying(buying.toXDR)
    op.setAmount(int64(selling.units))
    op.setPrice(price.toXDR)
    val body = new OperationBody
    body.setDiscriminant(MANAGE_OFFER)
    body.setManageOfferOp(op)
    body
  }
}

object ManageOfferOperation {
  def from(op: ManageOfferOp): Try[ManageOfferOperation] = for {
    selling <- Asset.fromXDR(op.getSelling)
    buying <- Asset.fromXDR(op.getBuying)
  } yield {
    val offerId = op.getOfferID.getUint64.longValue
    val amount = op.getAmount.getInt64.longValue
    val price = Price(
      n = op.getPrice.getN.getInt32.intValue,
      d = op.getPrice.getD.getInt32.intValue
    )
    (offerId, amount) match {
      case (0, _) => CreateOfferOperation(Amount(amount, selling), buying, price)
      case (_, 0) => DeleteOfferOperation(offerId, selling, buying, price)
      case _      => UpdateOfferOperation(offerId, Amount(amount, selling), buying, price)
    }
  }
}

case class Price(n: Int, d: Int) extends XDRPrimitives {
  def toXDR = {
    val xdr = new XDRPrice
    xdr.setN(int32(n))
    xdr.setD(int32(d))
    xdr
  }

}
