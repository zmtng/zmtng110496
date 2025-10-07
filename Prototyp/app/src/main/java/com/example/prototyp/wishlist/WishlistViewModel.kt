package com.example.prototyp.wishlist

import androidx.lifecycle.ViewModel
import com.example.prototyp.wishlist.WishlistDao

class WishlistViewModel(private val wishlistDao: WishlistDao) : ViewModel() {

    // Dieser Flow liefert die komplette Wunschliste, bereits verknüpft mit den Kartendetails.
    val wishlistCards = wishlistDao.observeWishlist()

    // Funktionen zum Hinzufügen/Verschieben kommen in den nächsten Schritten
}