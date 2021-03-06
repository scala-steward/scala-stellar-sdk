package stellar.sdk.model.response

import okio.ByteString
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model._
import stellar.sdk.model.op.CreateAccountOperation
import stellar.sdk.model.result._
import stellar.sdk.util.ByteArrays.bytesToHex

import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TransactionResponseSpec extends Specification with ArbitraryInput with DomainMatchers {

  val formatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .withZone(ZoneId.of("UTC"))

  "a transaction post response" should {
    "provide access to the signed transaction via XDR decoding" >> {
      TransactionApproved("", 1, "AAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAZAB16IkAAAABAAAAAAAAAAAAAAABAAAAA" +
        "AAAAAAAAAAAuRsw+AoWiSHa1TWuxE8O0ve5Ytj2JJE1sDrLNJspsxsAAAAAAJiWgAAAAAAAAAABDJm32AAAAEDnDn8POBeTu0v5Hj6VCVB" +
        "KABHtap9ut+HH0+taBQsDPNLA+WXfiwrq1hG5cEQP0qTHG59vkmyjxcejqjz7dPwO", "", "").transaction(TestNetwork) must
        beLike {
          case SignedTransaction(txn, signatures, feeBump) =>
            txn.source.id.encodeToChars.mkString mustEqual "GCLAOU4ANVI26AAI4ZM4Y4QI4PQW2EQELL3FAZE6MI7I2DYMTG35R35E"
            txn.source.sequenceNumber mustEqual 33188247383310337L
            txn.operations mustEqual Seq(
              CreateAccountOperation(
                KeyPair.fromAccountId("GC4RWMHYBILISIO22U225RCPB3JPPOLC3D3CJEJVWA5MWNE3FGZRXTFA").toAccountId,
                NativeAmount(10000000)
              )
            )
            txn.timeBounds must beEqualTo(Unbounded)
            txn.memo mustEqual NoMemo
            txn.maxFee mustEqual NativeAmount(100)
            signatures.map(_.data.toIndexedSeq).map(bytesToHex(_)) mustEqual Seq("E70E7F0F381793BB4BF91E3E950950" +
              "4A0011ED6A9F6EB7E1C7D3EB5A050B033CD2C0F965DF8B0AEAD611B970440FD2A4C71B9F6F926CA3C5C7A3AA3CFB74FC0E"
            )
            signatures.map(_.hint.toIndexedSeq).map(bytesToHex(_)) mustEqual Seq("0C99B7D8")
            feeBump must beNone
            txn.hash.hex().toUpperCase() mustEqual "BA68C0112AFE25A2FEA9A6E7926A4AEF9FF12FB627EC840840541813AAA695DB"
        }
    }

    "provide access to the XDR Transaction Result" >> {
      TransactionApproved("", 1, "", "AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=", "").result must
        beLike { case TransactionSuccess(feeCharged, opResults, hash) =>
          feeCharged.units mustEqual 100
          opResults mustEqual Seq(CreateAccountSuccess, PaymentNoDestination)
          hash mustEqual ByteString.EMPTY
        }
    }

    "consider sequence number updated on approval when the fee charged is not zero" >> {
      val approval = TransactionApproved("", 1, "", "AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=", "")
      approval.feeCharged mustEqual NativeAmount(100)
      approval.sequenceIncremented must beTrue
    }

    "consider sequence number updated on rejection when the fee charged is not zero" >> {
      val rejection = TransactionRejected(1, "", "", Nil, "", "AAAAAAAAAGT////7AAAAAA==")
      rejection.feeCharged mustEqual NativeAmount(100)
      rejection.sequenceIncremented must beTrue
    }

    "consider sequence number not updated on rejection when the fee charged is zero" >> {
      val rejection = TransactionRejected(1, "", "", Nil, "", "AAAAAAAAAAD////7AAAAAA==")
      rejection.feeCharged mustEqual NativeAmount(0)
      rejection.sequenceIncremented must beFalse
    }

    "deserialise from JSON" >> prop { tpr: TransactionApproved =>
      val json = ("hash" -> tpr.hash) ~ ("ledger" -> tpr.ledger) ~ ("envelope_xdr" -> tpr.envelopeXDR) ~
        ("result_xdr" -> tpr.resultXDR) ~ ("result_meta_xdr" -> tpr.resultMetaXDR)

      implicit val fmt = org.json4s.DefaultFormats + TransactionPostResponseDeserializer
      json.extract[TransactionPostResponse] mustEqual tpr
    }
  }

  "a transaction history" should {
    "deserialise from JSON" >> prop { h: TransactionHistory =>

      def accountId(accnId: AccountId, prefix: String): Option[JObject] =
        accnId.subAccountId.map(id =>
          (prefix -> accnId.publicKey.accountId) ~ (s"${prefix}_muxed_id" -> s"$id")
        ).orElse(Some(prefix -> accnId.publicKey.accountId))

      val sourceAccount: Option[JObject] = accountId(h.account, "source_account")
      val feeAccount: Option[JObject] = accountId(h.account, "fee_account")

      val feeBump: Option[JObject] = h.feeBump.map { bump =>
        ("fee_bump_transaction" -> (
          ("hash" -> bump.hash) ~
            ("signatures" -> bump.signatures))) ~
          ("inner_transaction" -> (
            ("max_fee" -> h.maxFee.units) ~
              ("hash" -> h.hash) ~
              ("signatures" -> h.signatures)
            ))
      }

      val ((memoType: String, memo: Option[(String, String)]), memoSecondary: Option[(String, String)]) = h.memo match {
        case NoMemo => "none" -> None -> None
        case MemoId(id) => "id" -> Some("memo" -> java.lang.Long.toUnsignedString(id)) -> None
        case MemoText(t) => "text" -> Some("memo" -> new String(t.toByteArray)) -> Some("memo_bytes" -> t.base64())
        case MemoHash(bs) => "hash" -> Some("memo" -> bs.base64()) -> None
        case MemoReturnHash(bs) => "return" -> Some("memo" -> bs.base64()) -> None
      }

      val base = ("hash" -> h.feeBump.map(_.hash).getOrElse(h.hash)) ~
        ("ledger" -> h.ledgerId) ~
        ("created_at" -> formatter.format(h.createdAt)) ~
        ("source_account_sequence" -> h.sequence) ~
        ("max_fee" -> h.feeBump.map(_.maxFee).getOrElse(h.maxFee).units) ~
        ("fee_charged" -> h.feeCharged.units) ~
        ("operation_count" -> h.operationCount) ~
        ("signatures" -> h.feeBump.map(_.signatures).getOrElse(h.signatures)) ~
        ("memo_type" -> memoType) ~
        ("envelope_xdr" -> h.envelopeXDR) ~
        ("result_xdr" -> h.resultXDR) ~
        ("result_meta_xdr" -> h.resultMetaXDR) ~
        ("fee_meta_xdr" -> h.feeMetaXDR) ~
        ("valid_after" -> h.validAfter.map(formatter.format)) ~
        ("valid_before" -> h.validBefore.map(formatter.format))

      val combineObj: (JObject, JObject) => JObject = (js, part) => js ~ part
      val combineStr: (JObject, (String, String)) => JObject = (js, part) => js ~ part

      val json: JObject =
        feeAccount.foldLeft(
          sourceAccount.foldLeft(
            feeBump.foldLeft(
              memo.foldLeft(
                memoSecondary.foldLeft(base)(combineStr)
              )(combineStr)
            )(combineObj)
          )(combineObj)
        )(combineObj)

      implicit val fmt = org.json4s.DefaultFormats + TransactionHistoryDeserializer
      json.extract[TransactionHistory] must beEquivalentTo(h)
    }
  }

}
