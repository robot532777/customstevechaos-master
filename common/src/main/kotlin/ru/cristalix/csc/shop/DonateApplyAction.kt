package ru.cristalix.csc.shop

sealed class DonateApplyAction {
    object AddPermanent : DonateApplyAction()
    class AddMoney(val amount: Int) : DonateApplyAction()
    object AddNewYearPack : DonateApplyAction()
}
