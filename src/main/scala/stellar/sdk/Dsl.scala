package stellar.sdk

import stellar.sdk.model.op.{CreateAccountOperation, Operation, PaymentOperation}
import stellar.sdk.model.{Account, Amount, NativeAmount, Transaction}

object Dsl {

  object Create {
    def account(publicKey: PublicKeyOps) = new {
      def withOpeningBalanceOf(lumens: NativeAmount) = CreateAccountOperation(publicKey, lumens)
    }
  }

  object Pay {
    def account(publicKey: PublicKeyOps) = new {
      def amountOf(amount: Amount) = PaymentOperation(publicKey, amount)
    }
  }

  def Lumens(l: Int) = Amount.lumens(l)

  def transact(ops: Operation*) = {
    // TODO(jem) - resolve the account
    val accn: Account = ???
    transact(accn)(ops: _*)
  }

  def transact(sourceAccount: Account)(ops: Operation*) = new {
    def signed(primarySigner: KeyPair, additionalSigners: KeyPair*): Transaction = {
      Transaction(sourceAccount, ops.toSeq)
    }
  }
}


class OperationBuilder {

  def build: Seq[Operation] = ???
}
