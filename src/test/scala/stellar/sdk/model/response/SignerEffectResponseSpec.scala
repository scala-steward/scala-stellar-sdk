package stellar.sdk.model.response

import java.time.ZonedDateTime
import java.util.Locale
import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.{AccountId, Amount}
import stellar.sdk.model.op.JsonSnippets

class SignerEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a signer created effect document" should {
    "parse to a signer created effect" >> prop { (id: String, created: ZonedDateTime, accnId: AccountId, weight: Short, pubKey: String) =>
      val json = doc(id, created, accnId, "signer_created", weight, "public_key" -> pubKey)
      parse(json).extract[EffectResponse] mustEqual EffectSignerCreated(id, created, accnId, weight, pubKey)
    }.setGen1(Gen.identifier).setGen5(Gen.identifier)
  }

  "a signer updated effect document" should {
    "parse to a signer updated effect" >> prop { (id: String, created: ZonedDateTime, accnId: AccountId, weight: Short, pubKey: String) =>
      val json = doc(id, created, accnId, "signer_updated", weight, "public_key" -> pubKey)
      parse(json).extract[EffectResponse] mustEqual EffectSignerUpdated(id, created, accnId, weight, pubKey)
    }.setGen1(Gen.identifier).setGen5(Gen.identifier)
  }

  "a signer removed effect document" should {
    "parse to a signer removed effect" >> prop { (id: String, created: ZonedDateTime, accnId: AccountId, pubKey: String) =>
      val json = doc(id, created, accnId, "signer_removed", 0, "public_key" -> pubKey)
      parse(json).extract[EffectResponse] mustEqual EffectSignerRemoved(id, created, accnId, pubKey)
    }.setGen1(Gen.identifier).setGen4(Gen.identifier)
  }

  "a signer sponsorship created effect document" should {
    "parse to a signer sponsorship created effect" >> prop { (id: String, created: ZonedDateTime, accnId: AccountId, signer: KeyPair, sponsor: KeyPair) =>
      val json = doc(id, created, accnId, "signer_sponsorship_created", 0, "signer" -> signer.accountId, "sponsor" -> sponsor.accountId)
      parse(json).extract[EffectResponse] mustEqual EffectSignerSponsorshipCreated(id, created, accnId, signer.asPublicKey, sponsor.asPublicKey)
    }.setGen1(Gen.identifier)
  }

  "a signer sponsorship removed effect document" should {
    "parse to a signer sponsorship removed effect" >> prop { (id: String, created: ZonedDateTime, accnId: AccountId, signer: KeyPair, sponsor: KeyPair) =>
      val json = doc(id, created, accnId, "signer_sponsorship_removed", 0, "signer" -> signer.accountId, "former_sponsor" -> sponsor.accountId)
      parse(json).extract[EffectResponse] mustEqual EffectSignerSponsorshipRemoved(id, created, accnId, signer.asPublicKey, sponsor.asPublicKey)
    }.setGen1(Gen.identifier)
  }

  "a signer sponsorship updated effect document" should {
    "parse to a signer sponsorship updated effect" >> prop { (id: String, created: ZonedDateTime, accnId: AccountId, signer: KeyPair, oldSponsor: KeyPair, newSponsor: KeyPair) =>
      val json = doc(id, created, accnId, "signer_sponsorship_updated", 0, "signer" -> signer.accountId, "former_sponsor" -> oldSponsor.accountId, "new_sponsor" -> newSponsor.accountId)
      parse(json).extract[EffectResponse] mustEqual EffectSignerSponsorshipUpdated(id, created, accnId, signer.asPublicKey, oldSponsor.asPublicKey, newSponsor.asPublicKey)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, created: ZonedDateTime, accnId: AccountId, tpe: String, weight: Short, extra: (String, Any)*) =
    s"""
       |{
       |  "id": "$id",
       |  "paging_token": "10157597659144-2",
       |  "account": "${accnId.publicKey.accountId}",
       |  ${accnId.subAccountId.map(id => s""""account_muxed_id": "$id",""").getOrElse("")}
       |  "weight": $weight
       |  "created_at": "${formatter.format(created)}",
       |  "type": "$tpe",
       |  "type_i": 10,
       |  ${extra.map {
              case (k, v: String) => s""""$k": "$v"""".trim
              case (k, v) => s""""$k": $v""".trim
          }.mkString(", ") }
       |}
    """.stripMargin

}
