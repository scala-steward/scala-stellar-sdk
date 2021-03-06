package stellar.sdk.model.result
import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{ClaimClaimableBalanceResultCode, OperationType, ClaimClaimableBalanceResult => XClaimClaimableBalanceResult}

sealed abstract class ClaimClaimableBalanceResult extends ProcessedOperationResult {
  def result: XClaimClaimableBalanceResult
  def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.CLAIM_CLAIMABLE_BALANCE)
    .claimClaimableBalanceResult(result)
    .build()
}

object ClaimClaimableBalanceResult {
  def decodeXdr(xdr: XClaimClaimableBalanceResult): ClaimClaimableBalanceResult = xdr.getDiscriminant match {
    case ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_SUCCESS => ClaimClaimableBalanceSuccess
    case ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_DOES_NOT_EXIST => ClaimClaimableBalanceDoesNotExist
    case ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_CANNOT_CLAIM => ClaimClaimableBalanceCannotClaim
    case ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_LINE_FULL => ClaimClaimableBalanceLineFull
    case ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_NO_TRUST => ClaimClaimableBalanceNoTrust
    case ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_NOT_AUTHORIZED => ClaimClaimableBalanceNotAuthorized
  }
}

/**
 * ClaimClaimableBalance operation was successful.
 */
case object ClaimClaimableBalanceSuccess extends ClaimClaimableBalanceResult {
  override def result: XClaimClaimableBalanceResult = new XClaimClaimableBalanceResult.Builder()
    .discriminant(ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_SUCCESS)
    .build()
}

/**
 * ClaimClaimableBalance operation failed because the balance did not exist
 */
case object ClaimClaimableBalanceDoesNotExist extends ClaimClaimableBalanceResult {
  override def result: XClaimClaimableBalanceResult = new XClaimClaimableBalanceResult.Builder()
    .discriminant(ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_DOES_NOT_EXIST)
    .build()
}

/**
 * ClaimClaimableBalance operation failed because the balance could not be claimed
 */
case object ClaimClaimableBalanceCannotClaim extends ClaimClaimableBalanceResult {
  override def result: XClaimClaimableBalanceResult = new XClaimClaimableBalanceResult.Builder()
    .discriminant(ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_CANNOT_CLAIM)
    .build()
}

/**
 * ClaimClaimableBalance operation failed because the trustline is full
 */
case object ClaimClaimableBalanceLineFull extends ClaimClaimableBalanceResult {
  override def result: XClaimClaimableBalanceResult = new XClaimClaimableBalanceResult.Builder()
    .discriminant(ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_LINE_FULL)
    .build()
}

/**
 * ClaimClaimableBalance operation failed because the required trustline is not present
 */
case object ClaimClaimableBalanceNoTrust extends ClaimClaimableBalanceResult {
  override def result: XClaimClaimableBalanceResult = new XClaimClaimableBalanceResult.Builder()
    .discriminant(ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_NO_TRUST)
    .build()
}

/**
 * ClaimClaimableBalance operation failed because the requester was not authorised
 */
case object ClaimClaimableBalanceNotAuthorized extends ClaimClaimableBalanceResult {
  override def result: XClaimClaimableBalanceResult = new XClaimClaimableBalanceResult.Builder()
    .discriminant(ClaimClaimableBalanceResultCode.CLAIM_CLAIMABLE_BALANCE_NOT_AUTHORIZED)
    .build()
}