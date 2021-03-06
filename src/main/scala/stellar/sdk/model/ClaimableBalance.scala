package stellar.sdk.model

import java.time.Instant

import okio.ByteString
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats, JObject}
import org.stellar.xdr.ClaimableBalanceID
import stellar.sdk.model.ClaimableBalance.parseClaimableBalance
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.{KeyPair, PublicKeyOps}

case class ClaimableBalance(
  id: ClaimableBalanceId,
  amount: Amount,
  sponsor: PublicKeyOps,
  claimants: List[Claimant],
  lastModifiedLedger: Long,
  lastModifiedTime: Instant
)

object ClaimableBalance {
  implicit val formats: Formats = DefaultFormats + ClaimantDeserializer

  def parseClaimableBalance(o: JObject): ClaimableBalance = {
    val idString = (o \ "id").extract[String]
    val value = ClaimableBalanceHashId(ByteString.decodeHex(idString.drop(8)))
    ClaimableBalance(
      id = value,
      amount = Amount.parseAmount(o),
      sponsor = KeyPair.fromAccountId((o \ "sponsor").extract[String]),
      claimants = (o \ "claimants").extract[List[Claimant]],
      lastModifiedLedger = (o \ "last_modified_ledger").extract[Long],
      lastModifiedTime = Instant.parse((o \ "last_modified_time").extract[String])
    )
  }
}

object ClaimableBalanceDeserializer extends ResponseParser[ClaimableBalance](parseClaimableBalance)