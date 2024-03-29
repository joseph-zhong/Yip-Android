/*
* Copyright (C) 2011 - 2015 by Ngewi Fet <ngewif@gmail.com>
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/

package com.example.joseph.yipandroid3;

public interface OnContactSelectedListener {

	/**
	 * Callback when the contact is selected from the list of contacts
	 * @param contactId Long ID of the contact which was selected. 
	 */
	void onContactNameSelected(long contactId);
	
	/**
	 * Callback when the contact number is selected from the contact details view
	 * @param contactNumber String with the number which was selected
	 * @param contactName Name of the contact which was selected as String
	 */
	void onContactNumberSelected(String contactNumber, String contactName);
}
