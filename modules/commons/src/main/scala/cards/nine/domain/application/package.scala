package cards.nine.domain

package object application {
  type BasicCardList = CardList[BasicCard]
  type FullCardList = CardList[FullCard]
}