[![](https://jitpack.io/v/ozanlimited/slidable-counter-button.svg)](https://jitpack.io/#ozanlimited/slidable-counter-button)
# Slidable Counter Button

![GIF](https://user-images.githubusercontent.com/22769589/102698965-625dd480-4252-11eb-87ab-f43134704e09.gif)

## Installation
Step 1. Add the JitPack repository to your build file
```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
Step 2. Add the dependency
```gradle
dependencies {
	implementation 'com.github.ozanlimited:slidable-counter-button:1.1.1'
}
```

## Basic Usage
```kotlin
counterButton.setup(
            SlidableCounterButtonViewState("My Awesome Product", 499.99, "$", 5),
            SlidableCounterButtonState.STATE_COLLAPSED
        )
```

## XML Attributes
<table>
<thead>
  <tr>
    <th>XML Attribute</th>
    <th>Format</th>
    <th>Description</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>app:animOnStart</td>
    <td>boolean</td>
    <td>Apply [R.anim.anim_bounce_left_to_right] on start. Just works with [STATE_COLLAPSED]</td>
  </tr>
  <tr>
    <td>app:makeRollAnimation</td>
    <td>boolean</td>
    <td>Make roll animation on count changed.</td>
  </tr>
  <tr>
    <td>app:minusActiveDrawable</td>
    <td>reference</td>
    <td>Set minus active drawable.</td>
  </tr>
  <tr>
    <td>app:minusInactiveDrawable</td>
    <td>reference</td>
    <td>Set minus inactive drawable.</td>
  </tr>
  <tr>
    <td>app:plusActiveDrawable</td>
    <td>reference</td>
    <td>Set plus active drawable.</td>
  </tr>
  <tr>
    <td>app:plusInactiveDrawable</td>
    <td>integer</td>
    <td>Set plus inactive drawable.</td>
  </tr>
  <tr>
    <td>app:defaultTextColor</td>
    <td>color</td>
    <td>Set default text color.</td>
  </tr>
  <tr>
    <td>app:accentTextColor</td>
    <td>color</td>
    <td>Set accent text color.</td>
  </tr>
  <tr>
    <td>app:cardViewTopBackgroundColor</td>
    <td>color</td>
    <td>Set top card's background color.</td>
  </tr>
  <tr>
    <td>app:cardViewBottomBackgroundColor</td>
    <td>color</td>
    <td>Set bottom card's background color.</td>
  </tr>
</tbody>
</table>

## For formatting your price label you can use [PriceFormatter](https://github.com/ozanlimited/slidable-counter-button/blob/master/lib/src/main/java/com/ozan/lib/slidablecounterbutton/PriceFormatter.kt)

```kotlin
counterButton.setPriceFormatter(object : PriceFormatter() {
            override fun getFormattedValue(price: Double, pieceValueSign: String?): CharSequence =
                // TODO - Format price
        })
```

## You can use [ValueChangedListener](https://github.com/ozanlimited/slidable-counter-button/blob/4d46f3f8a0c877463726e89a65166b5031f2d488/lib/src/main/java/com/ozan/lib/slidablecounterbutton/SlidableCounterButton.kt#L1010) for observing value chages.

```kotlin
counterButton.setValueChangedListener(object : SlidableCounterButton.ValueChangedListener {

            override fun onValueIncreased(count: Int, currentState: SlidableCounterButtonState) {
                // TODO - Value increased.
            }

            override fun onValueDecreased(count: Int, currentState: SlidableCounterButtonState) {
                // TODO - Value decreased.
            }

        })
```

## Also, you can use [OutOfStockListener](https://github.com/ozanlimited/slidable-counter-button/blob/4d46f3f8a0c877463726e89a65166b5031f2d488/lib/src/main/java/com/ozan/lib/slidablecounterbutton/SlidableCounterButton.kt#L1015) for handling out of stock situation.

```kotlin
counterButton.setValueChangedListener(object : SlidableCounterButton.ValueChangedListener {

            override fun onValueIncreased(count: Int, currentState: SlidableCounterButtonState) {
                // TODO - Value increased.
            }

            override fun onValueDecreased(count: Int, currentState: SlidableCounterButtonState) {
                // TODO - Value decreased.
            }

        })
```
