package util

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLSelectElement

fun inputValue(elementId: String) = inputElement(elementId).value

fun inputElement(elementId: String) = document.getElementById(elementId) as HTMLInputElement

fun selectorElement(elementId: String) = document.getElementById(elementId) as HTMLSelectElement

fun divElement(elementId: String) = document.getElementById(elementId) as HTMLDivElement

fun buttonElement(elementId: String) = document.getElementById(elementId) as HTMLButtonElement

fun pElement(elementId: String) = document.getElementById(elementId) as HTMLParagraphElement