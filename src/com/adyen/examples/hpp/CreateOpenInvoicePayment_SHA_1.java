package com.adyen.examples.hpp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

/**
 * Create an open invoice Payment (Klarna) On Hosted Payment Page (HPP)
 * 
 * The Adyen Hosted Payment Pages (HPPs) provide a flexible, secure and easy way to allow shoppers to pay for goods or
 * services. By submitting the form generated by this servlet to our HPP a payment will be created for the shopper.
 * 
 * @link /1.HPP/CreateOpenInvoicePaymentOnHpp
 * @author Created by Adyen - Payments Made Easy
 */

@WebServlet(urlPatterns = { "/1.HPP/CreateOpenInvoicePaymentOnHppSHA1" })
public class CreateOpenInvoicePayment_SHA_1 extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/**
		 * General HPP settings
		 * - hppUrl: URL of the Adyen HPP to submit the form to
		 * - hmacKey: shared secret key used to encrypt the signature
		 * 
		 * Both variables are dependent on the environment which should be used (Test/Live).
		 * HMAC key can be set up: Adyen CA >> Skins >> Choose your Skin >> Edit Tab >> Edit HMAC key for Test & Live.
		 */
		String hppUrl = "https://test.adyen.com/hpp/details.shtml";
		String hmacKey = "YourHmacSecretKey";

		// Generate dates
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime(); // current date
		calendar.add(Calendar.DATE, 1);
		Date sessionDate = calendar.getTime(); // current date + 1 day
		calendar.add(Calendar.DATE, 2);
		Date shippingDate = calendar.getTime(); // current date + 3 days

		// Define variables
		String merchantReference = "TEST-PAYMENT-" + new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(currentDate);
		String paymentAmount = "10000000";
		String currencyCode = "EUR";
		String shipBeforeDate = new SimpleDateFormat("yyyy-MM-dd").format(shippingDate);
		String skinCode = "YourSkinCode";
		String merchantAccount = "YourMerchantAccount";
		String sessionValidity = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(sessionDate);
		String shopperLocale = "en_US";
		String orderData = compressString("Orderdata to display on the HPP can be put here");
		String countryCode = "DE";
		String shopperEmail = "YourShopperEmail";
		String shopperReference = "testShopperReference";
		String allowedMethods = "";
		String blockedMethods = "";
		String offset = "";
		String brandCode ="klarna";
		String issuerId = "";
		
		// billingAddress
		Map<String, String> billingAddress = new HashMap<>();
		
		billingAddress.put("billingAddress.city", "Neuss");
		billingAddress.put("billingAddress.country","DE");
		billingAddress.put("billingAddress.houseNumberOrName", "14");
		billingAddress.put("billingAddress.postalCode", "41460");
		billingAddress.put("billingAddress.stateOrProvince", "");
		billingAddress.put("billingAddress.street", "Hellersbergstraße");
		billingAddress.put("billingAddressType", "1");
	
		// billingAddressSig
		String signingBillingAddressSig =  billingAddress.get("billingAddress.street") +
						                   billingAddress.get("billingAddress.houseNumberOrName") + 
						                   billingAddress.get("billingAddress.city") + 
						                   billingAddress.get("billingAddress.postalCode") + 
						                   billingAddress.get("billingAddress.stateOrProvince") + 
						                   billingAddress.get("billingAddress.country");
		
		String billingAddressSig;
		try {
			billingAddressSig = calculateHMAC(hmacKey, signingBillingAddressSig);
			billingAddress.put("billingAddressSig", billingAddressSig);
		} catch (GeneralSecurityException e1) {
			throw new ServletException(e1);
		}
				
		// deliveryAddress
	        Map<String, String> deliveryAddress = new HashMap<>();
	        deliveryAddress.put("deliveryAddress.city", billingAddress.get("billingAddress.city"));
	        deliveryAddress.put("deliveryAddress.country", billingAddress.get("billingAddress.country"));
	        deliveryAddress.put("deliveryAddress.houseNumberOrName",  billingAddress.get("billingAddress.houseNumberOrName"));
	        deliveryAddress.put("deliveryAddress.postalCode", billingAddress.get("billingAddress.postalCode"));
	        deliveryAddress.put("deliveryAddress.stateOrProvince", billingAddress.get("billingAddress.stateOrProvince"));
	        deliveryAddress.put("deliveryAddress.street", billingAddress.get("billingAddress.street"));
	        deliveryAddress.put("deliveryAddressType", billingAddress.get("billingAddressType"));
        
		// deliveryAddressSig
		String signingDeliveryAddressSig =      
				billingAddress.get("billingAddress.street") +
                billingAddress.get("billingAddress.houseNumberOrName") + 
                billingAddress.get("billingAddress.city") + 
                billingAddress.get("billingAddress.postalCode") + 
                billingAddress.get("billingAddress.stateOrProvince") + 
                billingAddress.get("billingAddress.country");
		
		String deliveryAddressSig;
		try {
			deliveryAddressSig = calculateHMAC(hmacKey, signingDeliveryAddressSig);
			deliveryAddress.put("deliveryAddressSig", deliveryAddressSig);
		} catch (GeneralSecurityException e1) {
			throw new ServletException(e1);
		}
		
		// Shopper data
		Map<String, String> shopper = new HashMap<>();
		shopper.put("shopper.firstName", "Testperson-de");
		shopper.put("shopper.infix", "");
		shopper.put("shopper.lastName", "Approved");
		shopper.put("shopper.gender", "MALE");
		shopper.put("shopper.dateOfBirthDayOfMonth", "07");
		shopper.put("shopper.dateOfBirthMonth", "07");
		shopper.put("shopper.dateOfBirthYear", "1960");
		shopper.put("shopper.telephoneNumber", "01522113356");
		shopper.put("shopperType", "1");
		
		 // shopperSig
		 String signingShopper =  shopper.get("shopper.firstName") +
				                  shopper.get("shopper.infix") +
				                  shopper.get("shopper.lastName") +
				                  shopper.get("shopper.gender") +
				                  shopper.get("shopper.dateOfBirthDayOfMonth") +
				                  shopper.get("shopper.dateOfBirthMonth") +
				                  shopper.get("shopper.dateOfBirthYear") +
				                  shopper.get("shopper.telephoneNumber") ;
		 
	 	String shopperSig;
			
		try {
			shopperSig = calculateHMAC(hmacKey, signingShopper);
			shopper.put("shopperSig", shopperSig);
		} catch (GeneralSecurityException e1) {
			throw new ServletException(e1);
		}
		 
		// shopperInformation
		List<Map<String,String>> shopperInformation = new ArrayList<>();
		
		shopperInformation.add(billingAddress);
		shopperInformation.add(deliveryAddress);
		shopperInformation.add(shopper);
				
		// invoice lines
		HashMap<String, String> invoiceLines = new HashMap<>();
		
		invoiceLines.put("openinvoicedata.numberOfLines", "3");
		invoiceLines.put("openinvoicedata.refundDescription", merchantReference);
		
		invoiceLines.put("openinvoicedata.line1.currencyCode", "EUR");
		invoiceLines.put("openinvoicedata.line1.description", "Apples");
		invoiceLines.put("openinvoicedata.line1.itemAmount", "7860");
		invoiceLines.put("openinvoicedata.line1.itemVatAmount", "1117");
		invoiceLines.put("openinvoicedata.line1.itemVatPercentage", "1900");
		invoiceLines.put("openinvoicedata.line1.numberOfItems","1");
		invoiceLines.put("openinvoicedata.line1.vatCategory", "High");
		
		invoiceLines.put("openinvoicedata.line2.currencyCode", "EUR");
		invoiceLines.put("openinvoicedata.line2.description", "Pear");
		invoiceLines.put("openinvoicedata.line2.itemAmount", "6754");
		invoiceLines.put("openinvoicedata.line2.itemVatAmount", "1117");
		invoiceLines.put("openinvoicedata.line2.itemVatPercentage", "1900");
		invoiceLines.put("openinvoicedata.line2.numberOfItems","1");
		invoiceLines.put("openinvoicedata.line2.vatCategory", "High");
		
		invoiceLines.put("openinvoicedata.line3.currencyCode", "EUR");
		invoiceLines.put("openinvoicedata.line3.description", "Pineapple");
		invoiceLines.put("openinvoicedata.line3.itemAmount", "9876");
		invoiceLines.put("openinvoicedata.line3.itemVatAmount", "1117");
		invoiceLines.put("openinvoicedata.line3.itemVatPercentage", "1900");
		invoiceLines.put("openinvoicedata.line3.numberOfItems","1");
		invoiceLines.put("openinvoicedata.line3.vatCategory", "High");

		// Signing the open invoice data
		// merchantSig
		String signingString = 
				paymentAmount + 
				currencyCode + 
				shipBeforeDate + 
				merchantReference + 
				skinCode +
				merchantAccount + 
				sessionValidity + 
				shopperEmail + 
				shopperReference + 
				allowedMethods + 
				blockedMethods +
				billingAddress.get("billingAddressType") +
				deliveryAddress.get("deliveryAddressType") +
				shopper.get("shopperType") + 
				offset;
		
		String merchantSig;
		try {
			merchantSig = calculateHMAC(hmacKey, signingString);
		} catch (GeneralSecurityException e) {
			throw new ServletException(e);
		}
		
		// Open invoice data signing string
		// adding the merchantSig to the sortedMap
		Map<String, String> mapInvoiceLines = invoiceLines;
		TreeMap<String, String> sortedMap = new TreeMap<>(mapInvoiceLines);
		sortedMap.put("merchantSig", merchantSig);
	
		// open invoice signing string
		String signingOpeninvoicedata = couplingKeys(sortedMap);
				
		// invoice lines signature
		String openinvoicedataSig;
		try {
			openinvoicedataSig = calculateHMAC(hmacKey, signingOpeninvoicedata);
		} catch (GeneralSecurityException e) {
			throw new ServletException(e);
		}
				
		// adding the openinvoicedata.sig to the hasMap invoice lines
		invoiceLines.put("openinvoicedata.sig", openinvoicedataSig);
	
		// Set request parameters for use on the JSP page
		request.setAttribute("hppUrl", hppUrl);
		request.setAttribute("merchantReference", merchantReference);
		request.setAttribute("paymentAmount", paymentAmount);
		request.setAttribute("currencyCode", currencyCode);
		request.setAttribute("shipBeforeDate", shipBeforeDate);
		request.setAttribute("skinCode", skinCode);
		request.setAttribute("merchantAccount", merchantAccount);
		request.setAttribute("sessionValidity", sessionValidity);
		request.setAttribute("shopperLocale", shopperLocale);
		request.setAttribute("orderData", orderData);
		request.setAttribute("countryCode", countryCode);
		request.setAttribute("shopperEmail", shopperEmail);
		request.setAttribute("shopperReference", shopperReference);
		request.setAttribute("allowedMethods", allowedMethods);
		request.setAttribute("blockedMethods", blockedMethods);
		request.setAttribute("offset", offset);
		request.setAttribute("merchantSig", merchantSig);
		request.setAttribute("brandCode", brandCode);
		request.setAttribute("issuerId", issuerId);
	
		// setting attributes from shopperInfo to the request		
		for(int i = 0 ; i<shopperInformation.size(); i++){
			Set<Entry<String, String>> hashSet = shopperInformation.get(i).entrySet();
			for(Entry<String, String> entry: hashSet){
				request.setAttribute( entry.getKey(), entry.getValue());
			}
		}
		
		// setting attributes from invoice lines to the request
		Set<Entry<String, String>> invoiceLineshashSet = invoiceLines.entrySet();
		for(Entry<String, String> entry: invoiceLineshashSet){
			request.setAttribute( entry.getKey(), entry.getValue());
		}
		
		// Set correct character encoding
		response.setCharacterEncoding("UTF-8");

		// Forward request data to corresponding JSP page
		request.getRequestDispatcher("/1.HPP/create-payment-on-hpp.jsp").forward(request, response);
	}

	/**
	 * Generates GZIP compressed and Base64 encoded string.
	 */
	private String compressString(String input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(output);

		gzip.write(input.getBytes("UTF-8"));
		gzip.close();
		output.close();

		return Base64.encodeBase64String(output.toByteArray());
	}

	/**
	 * Computes the Base64 encoded signature using the HMAC algorithm with the SHA-1 hashing function.
	 */
	private String calculateHMAC(String hmacKey, String signingString) throws GeneralSecurityException, UnsupportedEncodingException {
		SecretKeySpec keySpec = new SecretKeySpec(hmacKey.getBytes(), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(keySpec);

		byte[] result = mac.doFinal(signingString.getBytes("UTF-8"));
		return Base64.encodeBase64String(result);
	}

	/** 
	 * Coupling the key pairs to sign the open invoice data over
	 * @param sortedMap of the key value pairs that need to be coupled
	 * @return String of keys
	 */
	
	private String couplingKeys(NavigableMap<String, String> sortedMap){
		
		// coupling key names
		String keys = "";
		
		// Set lastItem variable to check when at last item
		Entry<String, String> lastItem = sortedMap.lastEntry();
						
		for (Entry<String, String> entry : sortedMap.entrySet()) {
			if(!lastItem.equals(entry)) {
				keys += entry.getKey().trim() + ":";
			} else{
				keys += entry.getKey().trim() + "|";
			}
		}
		
		for (Entry<String, String> entry : sortedMap.entrySet()) {
			if(!lastItem.equals(entry)) {
				keys += entry.getValue().trim() + ":";
			} else{
				keys += entry.getValue().trim() ;
			}
		}
		
		return keys;
	} 
	

}
