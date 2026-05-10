/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.NewPasswordInputForm;
import org.eclipse.kura.web.client.ui.validator.GwtValidators;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiChannelModel;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class TabWirelessUi extends Composite implements NetworkTab {

    private static final String AUTOMATIC_CHANNEL_DESCRIPTION = "Automatic";
    private static final String WORLD_REGION_COUNTRY_CODE = "00";

    private static final String STATUS_TABLE_ROW = "status-table-row";
    private static final String WIFI_MODE_STATION = GwtWifiWirelessMode.netWifiWirelessModeStation.name();
    private static final String WIFI_MODE_AP = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name();
    private static final String WIFI_MODE_STATION_MESSAGE = MessageUtils.get(WIFI_MODE_STATION);
    private static final String WIFI_MODE_ACCESS_POINT_MESSAGE = MessageUtils.get(WIFI_MODE_AP);
    private static final String WIFI_SECURITY_NONE_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityNONE.name());
    private static final String WIFI_SECURITY_WEP_MESSAGE = MessageUtils.get(GwtWifiSecurity.netWifiSecurityWEP.name());
    private static final String WIFI_SECURITY_WPA_MESSAGE = MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name());
    private static final String WIFI_SECURITY_WPA2_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA2.name());
    private static final String WIFI_SECURITY_WPA3_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA3.name());
    private static final String WIFI_SECURITY_WPA2_WPA3_ENTERPRISE_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA2WPA3Enterprise.name());
    private static final String WIFI_SECURITY_WPA_WPA2_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name());
    private static final String WIFI_SECURITY_WPA2_WPA3_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA2_WPA3.name());
    private static final String WIFI_BGSCAN_NONE_MESSAGE = MessageUtils
            .get(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name());
    private static final String WIFI_CIPHERS_CCMP_TKIP_MESSAGE = MessageUtils
            .get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());

    private static final String WIFI_RADIO_BGN = GwtWifiRadioMode.netWifiRadioModeBGN.name();
    private static final String WIFI_RADIO_ANAC = GwtWifiRadioMode.netWifiRadioModeANAC.name();
    private static final String WIFI_RADIO_BG = GwtWifiRadioMode.netWifiRadioModeBG.name();
    private static final String WIFI_RADIO_B = GwtWifiRadioMode.netWifiRadioModeB.name();
    private static final String WIFI_RADIO_A = GwtWifiRadioMode.netWifiRadioModeA.name();

    private static final String WIFI_RADIO_BGN_MESSAGE = MessageUtils.get(WIFI_RADIO_BGN);

    private static final String WIFI_BAND_5GHZ_MESSAGE = MessageUtils.get("netWifiBand5Ghz");
    private static final String WIFI_BAND_2GHZ_MESSAGE = MessageUtils.get("netWifiBand2Ghz");
    private static final String WIFI_BAND_BOTH_MESSAGE = MessageUtils.get("netWifiBandBoth");

    private static final String IPV4_STATUS_WAN_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusEnabledWAN.name());

    private static TabWirelessUiUiBinder uiBinder = GWT.create(TabWirelessUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(TabWirelessUi.class.getSimpleName());

    interface TabWirelessUiUiBinder extends UiBinder<Widget, TabWirelessUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    private static final String REGEX_PASS_WPA = "^[ -~]{8,63}$";
    private static final String REGEX_PASS_WEP = "^(?:[\\x00-\\x7F]{5}|[\\x00-\\x7F]{13}|[a-fA-F0-9]{10}|[a-fA-F0-9]{26})$";
    private static final String REGEX_WIFI_SID = "^[^!#;+\\]/\"\\t][^+\\]/\"\\t]{0,31}$";
    private static final int MAX_SSID_LENGTH = 32;

    private static final String PLACEHOLDER = "Placeholder";

    private final GwtSession session;
    private final TabIp4Ui tcp4Tab;
    private final TabIp6Ui tcp6Tab;
    private final NetworkTabsUi netTabs;
    private final ListDataProvider<GwtWifiHotspotEntry> ssidDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtWifiHotspotEntry> ssidSelectionModel = new SingleSelectionModel<>();

    AnchorListItem wireless8021xTabAnchorItem;

    private boolean dirty;
    private boolean ssidInit;
    private GwtWifiNetInterfaceConfig selectedNetIfConfig;
    private String tcp4Status;
    private String tcp6Status;
    private AtomicBoolean isWPA3Supported = new AtomicBoolean(false);

    GwtWifiConfig activeConfig;
    GwtWifiChannelModel previousSelection;

    @UiField
    Alert noChannels;
    @UiField
    Text noChannelsText;

    @UiField
    Form form;

    @UiField
    FormLabel labelWireless;
    @UiField
    FormLabel labelSsid;
    @UiField
    FormLabel labelRadio;
    @UiField
    FormLabel labelSecurity;
    @UiField
    FormLabel labelPassword;
    @UiField
    FormLabel labelPairwise;
    @UiField
    FormLabel labelGroup;
    @UiField
    FormLabel labelBgscan;
    @UiField
    FormLabel labelRssi;
    @UiField
    FormLabel labelShortI;
    @UiField
    FormLabel labelLongI;
    @UiField
    FormLabel labelPing;
    @UiField
    FormLabel labelIgnore;
    @UiField
    FormLabel labelChannelList;
    @UiField
    FormLabel labelCountryCode;

    @UiField
    InlineRadio radio1;
    @UiField
    InlineRadio radio2;
    @UiField
    InlineRadio radio3;
    @UiField
    InlineRadio radio4;

    @UiField
    ListBox wireless;
    @UiField
    ListBox radio;
    @UiField
    ListBox security;
    @UiField
    ListBox pairwise;
    @UiField
    ListBox group;
    @UiField
    ListBox bgscan;
    @UiField
    ListBox channelList;

    @UiField
    TextBox ssid;
    @UiField
    TextBox shortI;
    @UiField
    TextBox longI;

    @UiField
    NewPasswordInputForm passwordInputForm;

    @UiField
    TextBox rssi;

    @UiField
    TextBox countryCode;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    Button buttonSsid;

    @UiField
    FormGroup groupRssi;
    @UiField
    FormGroup groupPassword;
    @UiField
    FormGroup groupWireless;
    @UiField
    FormGroup groupShortI;
    @UiField
    FormGroup groupLongI;

    @UiField
    HelpBlock helpWireless;
    @UiField
    HelpBlock helpPassword;
    @UiField
    HelpBlock helpShortI;
    @UiField
    HelpBlock helpLongI;

    @UiField
    Modal ssidModal;

    @UiField
    PanelHeader ssidTitle;

    @UiField
    CellTable<GwtWifiHotspotEntry> ssidGrid = new CellTable<>();

    @UiField
    Alert searching;
    @UiField
    Alert noSsid;
    @UiField
    Alert scanFail;

    @UiField
    Button buttonSsidForceUpdate;

    @UiField
    Text searchingText;
    @UiField
    Text noSsidText;
    @UiField
    Text scanFailText;

    @UiField
    HelpButton wirelessHelp;
    @UiField
    HelpButton ssidHelp;
    @UiField
    HelpButton radioHelp;
    @UiField
    HelpButton securityHelp;
    @UiField
    HelpButton passwordHelp;
    @UiField
    HelpButton pairwiseHelp;
    @UiField
    HelpButton groupHelp;
    @UiField
    HelpButton bgscanHelp;
    @UiField
    HelpButton rssiHelp;
    @UiField
    HelpButton shortIHelp;
    @UiField
    HelpButton longIHelp;
    @UiField
    HelpButton pingHelp;
    @UiField
    HelpButton ignoreHelp;
    @UiField
    HelpButton channelListHelp;
    @UiField
    HelpButton countryCodeHelp;
    @UiField
    Modal regDomErrorModal;
    @UiField
    Alert unavailableChannelError;
    @UiField
    Text unavailableChannelErrorText;

    public TabWirelessUi(GwtSession currentSession, TabIp4Ui tcp4, TabIp6Ui tcp6,
            AnchorListItem wireless8021xTabAnchorItem, NetworkTabsUi tabs) {
        this.ssidInit = false;
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tcp4Tab = tcp4;
        this.tcp6Tab = tcp6;
        this.netTabs = tabs;
        this.wireless8021xTabAnchorItem = wireless8021xTabAnchorItem;
        this.helpTitle.setText(MSGS.netHelpTitle());

        changeRadioModeToBand();

        initCommonItems();
        initStationModeItems();
        initHelpButtons();
        initRegDomErrorModal();
        setPasswordValidation();

        this.tcp4Tab.status.addChangeHandler(event -> evalActiveConfig());

        this.tcp6Tab.status.addChangeHandler(event -> evalActiveConfig());

        configureWifiSecurityListBox();

        logger.info("Constructor done.");
    }

    private void evalActiveConfig() {
        if (TabWirelessUi.this.selectedNetIfConfig != null) {
            // set the default values for wireless mode if tcp/ip status was changed
            String tcpIp4Status = TabWirelessUi.this.tcp4Tab.getStatus();
            String tcpIp6Status = TabWirelessUi.this.tcp6Tab.getStatus();
            boolean isStatusChanged = !tcpIp4Status.equals(TabWirelessUi.this.tcp4Status)
                    || !tcpIp6Status.equals(TabWirelessUi.this.tcp6Status);

            if (isStatusChanged) {
                if (tcpIp4Status.equals(MessageUtils.get(GwtNetIfStatus.netIPv4StatusEnabledWAN.name()))
                        || tcpIp6Status.equals(MessageUtils.get(GwtNetIfStatus.netIPv6StatusEnabledWAN.name()))) {
                    TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getStationWifiConfig();
                } else {
                    TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getActiveWifiConfig();
                }

                TabWirelessUi.this.tcp4Status = tcpIp4Status;
                TabWirelessUi.this.tcp6Status = tcpIp6Status;
                TabWirelessUi.this.netTabs.updateTabs();

                update();
            }
        }
    }

    @UiHandler(value = { "wireless", "ssid", "radio", "security", "passwordInputForm", "pairwise", "group", "bgscan",
            "longI", "shortI", "radio1", "radio2", "radio3", "radio4", "rssi", "channelList", "countryCode" })
    public void onChange(ChangeEvent e) {
        setDirty(true);
    }

    public GwtWifiWirelessMode getWirelessMode() {
        if (this.wireless != null) {
            for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                if (this.wireless.getSelectedItemText().equals(MessageUtils.get(mode.name()))) {
                    return mode;
                }
            }
        } else {
            if (this.activeConfig != null) {
                return GwtWifiWirelessMode.valueOf(this.activeConfig.getWirelessMode());
            }
        }
        return null;
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.netTabs.getButtons() != null) {
            this.netTabs.getButtons().setButtonsDirty(flag);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        boolean result = this.form.validate();
        result &= checkPassword();

        result = result && !this.groupWireless.getValidationState().equals(ValidationState.ERROR)
                && !this.groupPassword.getValidationState().equals(ValidationState.ERROR);

        if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText()) && !this.bgscan.getSelectedItemText()
                .equals(WIFI_BGSCAN_NONE_MESSAGE)) {
            result = result && !this.groupShortI.getValidationState().equals(ValidationState.ERROR)
                    && !this.groupLongI.getValidationState().equals(ValidationState.ERROR);
        } else {
            this.groupShortI.setValidationState(ValidationState.NONE);
            this.groupLongI.setValidationState(ValidationState.NONE);
        }

        return result;
    }

    @SuppressWarnings("all") // Ignoring warning gwt doesn't support the proposed Java syntax
    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        if (this.tcp4Status == null || this.tcp6Status == null || this.selectedNetIfConfig != config) {
            this.tcp4Status = this.tcp4Tab.getStatus();
            this.tcp6Status = this.tcp6Tab.getStatus();
        }
        if (config instanceof GwtWifiNetInterfaceConfig) {
            this.selectedNetIfConfig = (GwtWifiNetInterfaceConfig) config;

            this.activeConfig = this.selectedNetIfConfig.getActiveWifiConfig();
        }
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            if (this.selectedNetIfConfig == null) {
                resetForm();
            } else {
                update();
            }
        }

    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtWifiNetInterfaceConfig updatedWifiNetIf = (GwtWifiNetInterfaceConfig) updatedNetIf;

        if (this.session != null) {
            GwtWifiConfig updatedWifiConfig = getGwtWifiConfig();
            updatedWifiNetIf.setWirelessMode(updatedWifiConfig.getWirelessMode());

            // update the wifi config
            updatedWifiNetIf.setWifiConfig(updatedWifiConfig);
        } else {
            if (this.selectedNetIfConfig != null) {
                updatedWifiNetIf.setAccessPointWifiConfig(this.selectedNetIfConfig.getAccessPointWifiConfigProps());
                updatedWifiNetIf.setStationWifiConfig(this.selectedNetIfConfig.getStationWifiConfigProps());

                // select the correct mode
                for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                    if (mode.name().equals(this.selectedNetIfConfig.getWirelessMode())) {
                        updatedWifiNetIf.setWirelessMode(mode.name());
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        // Not needed
    }

    // -----Private methods-------//

    private void changeRadioModeToBand() {
        this.radioHelp.setHelpText(MSGS.netWifiToolTipBand());
        this.labelRadio.setText(MSGS.netWifiBand());
    }

    private void update() {
        setCurrentItemsValues();
        this.netTabs.updateTabs();
        refreshForm();
        setPasswordValidation();
    }

    private void setCurrentItemsValues() {

        if (this.activeConfig == null) {
            return;
        }

        for (int i = 0; i < this.wireless.getItemCount(); i++) {
            if (this.wireless.getItemText(i).equals(MessageUtils.get(this.activeConfig.getWirelessMode()))) {
                this.wireless.setSelectedIndex(i);
            }
        }

        this.ssid.setValue(GwtSafeHtmlUtils.htmlUnescape(this.activeConfig.getWirelessSsid()));

        setRadioModeByValue(this.activeConfig.getRadioMode());

        setCurrentSecurityValues();
        setCurrentGroupCiphersValues();
        setCurrentBgscanValues();
        setCurrentPingApValues();
        setCurrentIgnoreSsidValues();
    }

    private void setCurrentSecurityValues() {
        String activeSecurity = this.activeConfig.getSecurity();
        if (activeSecurity != null) {
            for (int i = 0; i < this.security.getItemCount(); i++) {
                if (this.security.getItemText(i).equals(MessageUtils.get(activeSecurity))) {
                    this.security.setSelectedIndex(i);
                    break;
                }
            }
        }

        this.passwordInputForm.setInputPasswordValue(this.activeConfig.getPassword());

        String activePairwiseCiphers = this.activeConfig.getPairwiseCiphers();
        if (activePairwiseCiphers != null) {
            for (int i = 0; i < this.pairwise.getItemCount(); i++) {
                if (this.pairwise.getItemText(i).equals(MessageUtils.get(activePairwiseCiphers))) {
                    this.pairwise.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void setCurrentGroupCiphersValues() {
        String activeGroupCiphers = this.activeConfig.getGroupCiphers();
        if (activeGroupCiphers != null) {
            for (int i = 0; i < this.group.getItemCount(); i++) {
                if (this.group.getItemText(i).equals(MessageUtils.get(activeGroupCiphers))) { // activeConfig.getGroupCiphers()
                    this.group.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void setCurrentBgscanValues() {
        String activeBgscanModule = this.activeConfig.getBgscanModule();
        if (activeBgscanModule != null) {
            for (int i = 0; i < this.bgscan.getItemCount(); i++) {
                if (this.bgscan.getItemText(i).equals(MessageUtils.get(activeBgscanModule))) {
                    this.bgscan.setSelectedIndex(i);
                    break;
                }
            }
        }

        this.rssi.setValue(String.valueOf(this.activeConfig.getBgscanRssiThreshold()));
        this.shortI.setValue(String.valueOf(this.activeConfig.getBgscanShortInterval()));
        this.longI.setValue(String.valueOf(this.activeConfig.getBgscanLongInterval()));
    }

    private void setCurrentPingApValues() {
        this.radio1.setValue(this.activeConfig.pingAccessPoint());
        this.radio2.setValue(!this.activeConfig.pingAccessPoint());
    }

    private void setCurrentIgnoreSsidValues() {
        this.radio3.setValue(this.activeConfig.ignoreSSID());
        this.radio4.setValue(!this.activeConfig.ignoreSSID());
    }

    private void setRadioModeByValue(String radioModeValue) {
        if (radioModeValue != null) {
            setBandFromRadioMode(radioModeValue);
        }
    }

    private void setBandFromRadioMode(String radioModeValue) {
        boolean isValue5GHz = radioModeValue.equals(WIFI_RADIO_ANAC) || radioModeValue.equals(WIFI_RADIO_A);
        boolean isValue2GHz = radioModeValue.equals(WIFI_RADIO_BG) || radioModeValue.equals(WIFI_RADIO_B);
        boolean isValueBoth = radioModeValue.equals(WIFI_RADIO_BGN);

        for (int i = 0; i < this.radio.getItemCount(); i++) {
            String selectedText = this.radio.getItemText(i);

            if (selectedText.equals(WIFI_BAND_5GHZ_MESSAGE) && isValue5GHz) {
                this.radio.setSelectedIndex(i);
            }

            if (selectedText.equals(WIFI_BAND_2GHZ_MESSAGE) && isValue2GHz) {
                this.radio.setSelectedIndex(i);
            }

            if (selectedText.equals(WIFI_BAND_BOTH_MESSAGE) && isValueBoth) {
                this.radio.setSelectedIndex(i);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updatePasswordItemVisibility() {
        // disable Password if security is none, or if Wifi-Enterprise is enabled
        boolean shouldPasswordItemBeEnabled = !this.security.getSelectedItemText()
                .equals(WIFI_SECURITY_NONE_MESSAGE)
                && !this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_WPA3_ENTERPRISE_MESSAGE);

        this.groupPassword.setValidationState(ValidationState.NONE);
        if (shouldPasswordItemBeEnabled) {
            this.passwordInputForm.setInputPasswordType(InputType.PASSWORD);
            this.passwordInputForm.setShowPasswordButtonIcon(IconType.EYE);
            this.passwordInputForm.setShowPasswordButtonEnabled(!PLACEHOLDER.equals(this.passwordInputForm.getInputPasswordText()));
            this.passwordInputForm.setShowPasswordButtonVisible(true);
            this.passwordInputForm.setInputPasswordEnabled(true);
            this.passwordInputForm.setInputPasswordVisible(true);
            this.passwordHelp.setVisible(true);
            this.labelPassword.setVisible(true);

        } else {
            this.passwordInputForm.setInputPasswordEnabled(false);
            this.passwordInputForm.setInputPasswordVisible(false);
            this.passwordHelp.setVisible(false);
            this.labelPassword.setVisible(false);
            this.passwordInputForm.setShowPasswordButtonEnabled(false);
            this.passwordInputForm.setShowPasswordButtonVisible(false);
            // Clear password validators and errors when hidden
            this.passwordInputForm.setInputPasswordValidators();
            this.helpPassword.setText("");
        }
    }

    private void refreshForm() {

        String tcpip4Status = this.tcp4Tab.getStatus();
        String tcpip6Status = this.tcp6Tab.getStatus();

        // Tcp/IP disabled
        if (tcpip4Status.equals(MessageUtils.get(GwtNetIfStatus.netIPv4StatusDisabled.name()))
                && tcpip6Status.equals(MessageUtils.get(GwtNetIfStatus.netIPv6StatusDisabled.name()))) {
            setForm(false);
        } else {
            setForm(true);

            refreshCommonWirelessFormItems();

            if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {

                refreshStationModeItems(tcpip4Status, tcpip6Status);

            } else if (WIFI_MODE_ACCESS_POINT_MESSAGE.equals(this.wireless.getSelectedItemText())) {

                refreshAccessPointModeItems(tcpip4Status, tcpip6Status);

            }
        }

        this.netTabs.updateTabs();
    }

    private void refreshCommonWirelessFormItems() {
        // Shared items between Station and Access Point mode

        this.radio.setEnabled(true);
        this.channelList.setEnabled(true);
        loadRadioMode();

        this.wireless8021xTabAnchorItem
                .setEnabled(this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_WPA3_ENTERPRISE_MESSAGE));

        updatePasswordItemVisibility();

        loadCountryCode();
    }

    private void setForm(boolean visible) {
        this.wireless.setEnabled(visible);
        this.ssid.setEnabled(visible);
        this.buttonSsid.setEnabled(visible);
        this.radio.setEnabled(visible);
        this.security.setEnabled(visible);
        this.pairwise.setEnabled(visible);
        this.group.setEnabled(visible);
        this.bgscan.setEnabled(visible);

        this.rssi.setEnabled(visible);
        this.shortI.setEnabled(visible);
        this.longI.setEnabled(visible);

        this.radio1.setEnabled(visible);
        this.radio2.setEnabled(visible);
        this.radio3.setEnabled(visible);
        this.radio4.setEnabled(visible);

        this.channelList.setEnabled(visible);
        this.noChannels.setVisible(false);

        this.countryCode.setVisible(visible);
        this.countryCode.setEnabled(false);
    }

    private void updatePairwiseAndGroupItemsVisibility(boolean isPairwiseEnabled, boolean isAccessPointMode) {
        // pairwise depends only on security for both AP and Station mode
        this.pairwise.setEnabled(isPairwiseEnabled);
        this.pairwise.setVisible(isPairwiseEnabled);
        this.pairwiseHelp.setVisible(isPairwiseEnabled);
        this.labelPairwise.setVisible(isPairwiseEnabled);
        // other items must be shown only in Station mode
        this.group.setEnabled(isPairwiseEnabled && !isAccessPointMode);
        this.group.setVisible(isPairwiseEnabled && !isAccessPointMode);
        this.groupHelp.setVisible(isPairwiseEnabled && !isAccessPointMode);
        this.labelGroup.setVisible(isPairwiseEnabled && !isAccessPointMode);
    }

    private void refreshAccessPointModeItems(String tcpip4Status, String tcpip6Status) {

        remove8021xFromSecurityDropdown();

        // disable access point when TCP/IP is set to WAN
        if (tcpip4Status.equals(IPV4_STATUS_WAN_MESSAGE) || tcpip6Status.equals(IPV4_STATUS_WAN_MESSAGE)) {
            setForm(false);
        }

        this.ssid.setEnabled(true);
        this.buttonSsid.setEnabled(false);

        this.bgscan.setEnabled(false);
        this.bgscan.setVisible(false);
        this.labelBgscan.setVisible(false);
        this.bgscanHelp.setVisible(false);
        this.rssi.setEnabled(false);
        this.rssi.setVisible(false);
        this.rssiHelp.setVisible(false);
        this.labelRssi.setVisible(false);
        this.shortI.setEnabled(false);
        this.shortI.setVisible(false);
        this.shortIHelp.setVisible(false);
        this.labelShortI.setVisible(false);
        this.longI.setEnabled(false);
        this.longI.setVisible(false);
        this.longIHelp.setVisible(false);
        this.labelLongI.setVisible(false);
        this.radio1.setEnabled(false);
        this.radio1.setVisible(false);
        this.radio2.setEnabled(false);
        this.radio2.setVisible(false);
        this.labelPing.setVisible(false);

        updatePairwiseAndGroupItemsVisibility(isWpaLikePairwiseSecurity(), true);
    }

    private void refreshStationModeItems(String tcpip4Status, String tcpip6Status) {
        add8021xFromSecurityDropdown();

        if (tcpip4Status.equals(IPV4_STATUS_WAN_MESSAGE) || tcpip6Status.equals(IPV4_STATUS_WAN_MESSAGE)) {
            this.wireless.setEnabled(false);
        }

        this.ssid.setEnabled(true);
        this.buttonSsid.setEnabled(true);

        this.bgscan.setEnabled(true);
        this.bgscan.setVisible(true);
        this.bgscanHelp.setVisible(true);
        this.labelBgscan.setVisible(true);

        enableBgScanItems(!this.bgscan.getSelectedItemText().equals(WIFI_BGSCAN_NONE_MESSAGE));

        updatePairwiseAndGroupItemsVisibility(isWpaLikePairwiseSecurity(), false);

        this.radio1.setEnabled(true);
        this.radio1.setVisible(true);
        this.radio2.setEnabled(true);
        this.radio2.setVisible(true);
        this.labelPing.setVisible(true);
    }

    private void enableBgScanItems(boolean isBgScanEnabled) {
        this.rssi.setVisible(isBgScanEnabled);
        this.rssiHelp.setVisible(isBgScanEnabled);
        this.labelRssi.setVisible(isBgScanEnabled);
        this.shortI.setVisible(isBgScanEnabled);
        this.shortIHelp.setVisible(isBgScanEnabled);
        this.labelShortI.setVisible(isBgScanEnabled);
        this.longI.setVisible(isBgScanEnabled);
        this.longIHelp.setVisible(isBgScanEnabled);
        this.labelLongI.setVisible(isBgScanEnabled);

        // Clear validation errors when fields become invisible
        if (!isBgScanEnabled) {
            this.groupShortI.setValidationState(ValidationState.NONE);
            this.helpShortI.setText("");
            this.groupLongI.setValidationState(ValidationState.NONE);
            this.helpLongI.setText("");
            this.groupRssi.setValidationState(ValidationState.NONE);
        }
    }

    private void resetForm() {

        for (int i = 0; i < this.wireless.getItemCount(); i++) {
            if (this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                this.wireless.setSelectedIndex(i);
            }
        }
        this.ssid.setText("");
        for (int i = 0; i < this.radio.getItemCount(); i++) {
            if (this.radio.getItemText(i).equals(WIFI_RADIO_BGN_MESSAGE)) {
                this.radio.setSelectedIndex(i);
            }
        }

        resetSecurityItems();

        for (int i = 0; i < this.group.getItemCount(); i++) {
            if (this.group.getItemText(i).equals(WIFI_CIPHERS_CCMP_TKIP_MESSAGE)) {
                this.group.setSelectedIndex(i);
            }
        }

        resetBgscanItems();

        this.radio2.setValue(true);
        this.radio4.setValue(true);

        if (this.selectedNetIfConfig != null) {
            update();
        }
    }

    private void resetSecurityItems() {
        for (int i = 0; i < this.security.getItemCount(); i++) {
            if (this.security.getItemText(i).equals(WIFI_SECURITY_WPA2_MESSAGE)) {
                this.security.setSelectedIndex(i);
            }
        }

        this.passwordInputForm.setInputPasswordText("");

        for (int i = 0; i < this.pairwise.getItemCount(); i++) {
            if (this.pairwise.getItemText(i).equals(WIFI_CIPHERS_CCMP_TKIP_MESSAGE)) {
                this.pairwise.setSelectedIndex(i);
            }
        }
    }

    private void resetBgscanItems() {
        for (int i = 0; i < this.bgscan.getItemCount(); i++) {
            if (this.bgscan.getItemText(i).equals(WIFI_BGSCAN_NONE_MESSAGE)) {
                this.bgscan.setSelectedIndex(i);
            }
        }

        this.rssi.setValue("");
        this.shortI.setValue("");
        this.longI.setValue("");
    }

    private void initHelpButtons() {
        this.wirelessHelp.setHelpTextProvider(() -> {
            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(MessageUtils.get(WIFI_MODE_STATION))) {
                return MSGS.netWifiToolTipWirelessModeStation();
            } else {
                return MSGS.netWifiToolTipWirelessModeAccessPoint();
            }
        });
        this.ssidHelp.setHelpText(MSGS.netWifiToolTipNetworkName());

        this.radioHelp.setHelpText(MSGS.netWifiToolTipRadioMode());
        this.securityHelp.setHelpText(MSGS.netWifiToolTipSecurity());
        this.passwordHelp.setHelpText(MSGS.netWifiToolTipPassword());
        this.shortIHelp.setHelpText(MSGS.netWifiToolTipBgScanShortInterval());
        this.longIHelp.setHelpText(MSGS.netWifiToolTipBgScanLongInterval());
        this.pingHelp.setHelpText(MSGS.netWifiToolTipPingAccessPoint());
        this.ignoreHelp.setHelpText(MSGS.netWifiToolTipIgnoreSSID());
        this.channelListHelp.setHelpText(MSGS.netWifiToolTipChannelList());
        this.countryCodeHelp.setHelpText(MSGS.netWifiToolTipCountryCode());
    }

    /*
     * Initialize common items between Station and Access Point mode
     */

    private void initCommonItems() {
        initWirelessModeItem();
        initNetworkNameItem();
        initRadioBandItem();
        initWifiSecurityListBoxItem();
        initChannelListItem();
        initPasswordItem();
        initPairwiseCiphersItem();
        initIgnoreBroadcastSsidItem();
        initCountryCodeItem();
    }

    private void initWirelessModeItem() {
        this.labelWireless.setText(MSGS.netWifiWirelessMode());
        this.wireless.addItem(WIFI_MODE_STATION_MESSAGE);
        this.wireless.addItem(WIFI_MODE_ACCESS_POINT_MESSAGE);
        this.wireless.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipWirelessModeStation()));
            } else {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipWirelessModeAccessPoint()));
            }
        });
        this.wireless.addMouseOutHandler(event -> resetHelp());

        this.wireless.addChangeHandler(event -> {
            setDirty(true);
            TabWirelessUi.this.helpWireless.setText("");
            TabWirelessUi.this.groupWireless.setValidationState(ValidationState.NONE);

            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getStationWifiConfig();
            } else {
                // use values from access point config
                TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getAccessPointWifiConfig();
            }
            TabWirelessUi.this.netTabs.updateTabs();

            updatePasswordItemVisibility();

            update();
            TabWirelessUi.this.wirelessHelp.updateHelpText();
        });
    }

    private void initNetworkNameItem() {
        this.labelSsid.setText(MSGS.netWifiNetworkName());
        this.labelSsid.setShowRequiredIndicator(true);
        this.ssid.setMaxLength(MAX_SSID_LENGTH);
        this.ssid.setAllowBlank(false);
        this.ssid.addValidator(GwtValidators.regex(REGEX_WIFI_SID, MSGS.netWifiWirelessInvalidSSID()));
        this.ssid.setValidateOnBlur(true);
        this.ssid.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.ssid.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipNetworkName()));
            }
        });
        this.ssid.addMouseOutHandler(event -> resetHelp());
        this.buttonSsid.addClickHandler(event -> {
            if (!TabWirelessUi.this.ssidInit) {
                initWirelessScanSsid();
                TabWirelessUi.this.ssidDataProvider.getList().clear();
                TabWirelessUi.this.searching.setVisible(true);
                TabWirelessUi.this.noSsid.setVisible(false);
                TabWirelessUi.this.ssidGrid.setVisible(false);
                TabWirelessUi.this.scanFail.setVisible(false);
            }
            initWirelessScanModal();
            loadWirelessScanData(false);
        });

        this.buttonSsidForceUpdate.addClickHandler(event -> {
            if (!TabWirelessUi.this.ssidInit) {
                initWirelessScanSsid();
                TabWirelessUi.this.ssidDataProvider.getList().clear();
                TabWirelessUi.this.searching.setVisible(true);
                TabWirelessUi.this.noSsid.setVisible(false);
                TabWirelessUi.this.ssidGrid.setVisible(false);
                TabWirelessUi.this.scanFail.setVisible(false);
            }
            initWirelessScanModal();
            loadWirelessScanData(true);
        });
    }

    private void initRadioBandItem() {
        this.labelRadio.setText(MSGS.netWifiBand());
        this.radio.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBand()));
            }
        });
        this.radio.addMouseOutHandler(event -> resetHelp());
        this.radio.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });
    }

    private void initWifiSecurityListBoxItem() {
        this.labelSecurity.setText(MSGS.netWifiWirelessSecurity());
        this.security.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.security.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText
                        .add(new Span(composeNetWifiToolTipSecurity(TabWirelessUi.this.isWPA3Supported.get())));
            }
        });
        this.security.addMouseOutHandler(event -> resetHelp());
        this.security.clear();
        for (GwtWifiSecurity mode : GwtWifiSecurity.values()) {
            if (mode.equals(GwtWifiSecurity.netWifiSecurityWPA3)
                    || mode.equals(GwtWifiSecurity.netWifiSecurityWPA2_WPA3)) {
                if (TabWirelessUi.this.isWPA3Supported.get()) {
                    this.security.addItem(MessageUtils.get(mode.name()));
                }
            } else {
                this.security.addItem(MessageUtils.get(mode.name()));
            }
        }
        this.security.addChangeHandler(event -> {
            setDirty(true);
            setPasswordValidation();

            updatePasswordItemVisibility();

            refreshForm();
            checkPassword();
        });
    }

    private void initChannelListItem() {
        this.labelChannelList.setText(MSGS.netWifiChannelList());
        this.channelList.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.channelList.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipChannelList()));
            }
        });
        this.channelList.addChangeHandler(event -> {
            TabWirelessUi.this.activeConfig.setChannels(
                    Collections.singletonList(getChannelValueByIndex(this.channelList.getSelectedIndex())));
            TabWirelessUi.this.activeConfig
                    .setChannelsFrequency(getChannelFrequencyByIndex(this.channelList.getSelectedIndex()));
        });
        this.channelList.addMouseOutHandler(event -> resetHelp());
    }

    private void initPasswordItem() {
        this.labelPassword.setText(MSGS.netWifiWirelessPassword());
        this.passwordInputForm.setInputPasswordMouseOverHandler(handler -> {
            if (TabWirelessUi.this.passwordInputForm.isInputPasswordEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPassword()));
            }
        });

        this.passwordInputForm.setInputPasswordBlurHandler(handler -> this.passwordInputForm.validateInputPassword());
        this.passwordInputForm.setInputPasswordAllowBlank(true);
        this.passwordInputForm.setInputPasswordMouseOutHandler(handler -> resetHelp());

        this.passwordInputForm.setInputPasswordKeyUpHandler(handler -> this.passwordInputForm.validateInputPassword());
        this.passwordInputForm.setInputPasswordChangeHandler(handler -> {
            refreshForm();
            checkPassword();
        });

        this.passwordInputForm.setInputPasswordType(InputType.PASSWORD);
        this.passwordInputForm.setInputPasswordClickHandler(handler -> {
            if (TabWirelessUi.this.passwordInputForm.isInputPasswordEnabled()
                    && TabWirelessUi.this.passwordInputForm.getInputPasswordText().equals(PLACEHOLDER)) {
                TabWirelessUi.this.passwordInputForm.setInputPasswordText("");
                this.passwordInputForm.validateInputPassword();
                this.passwordInputForm.setShowPasswordButtonEnabled(true);
            }
        });

        // Show Password button
        updatePasswordItemVisibility();

        this.passwordInputForm.setShowPasswordButtonMouseOverHandler(handler -> {
            TabWirelessUi.this.helpText.clear();
            TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipShowButtonPassword()));
        });
    }

    private void initPairwiseCiphersItem() {
        this.labelPairwise.setText(MSGS.netWifiWirelessPairwiseCiphers());
        this.pairwise.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.pairwise.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPairwiseCiphers()));
            }
        });
        this.pairwise.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiCiphers cipher : GwtWifiCiphers.values()) {
            if (GwtWifiCiphers.netWifiCiphers_NONE == cipher) {
                continue;
            }
            this.pairwise.addItem(MessageUtils.get(cipher.name()));
        }
        this.pairwise.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });
    }

    private void initIgnoreBroadcastSsidItem() {

        this.labelIgnore.setText(MSGS.netWifiWirelessIgnoreSSID());
        this.radio3.setText(MSGS.trueLabel());
        this.radio3.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio3.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
            }
        });
        this.radio3.addMouseOutHandler(event -> resetHelp());
        this.radio3.addChangeHandler(event -> setDirty(true));
        this.radio4.setText(MSGS.falseLabel());
        this.radio4.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio4.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
            }
        });
        this.radio4.addMouseOutHandler(event -> resetHelp());
        this.radio4.addChangeHandler(event -> setDirty(true));
    }

    private void initCountryCodeItem() {
        this.labelCountryCode.setText(MSGS.netWifiCountryCodeLabel());
        this.noChannelsText.setText(MSGS.netWifiAlertNoChannels());
    }

    /*
     * Initialize Station Mode specific items
     */

    private void initStationModeItems() {
        initGroupCiphersItem();
        initBgScanItems();
        initAccessPointPingItems();
    }

    private void initGroupCiphersItem() {
        this.labelGroup.setText(MSGS.netWifiWirelessGroupCiphers());
        this.group.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.group.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipGroupCiphers()));
            }
        });
        this.group.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiCiphers cipher : GwtWifiCiphers.values()) {
            if (GwtWifiCiphers.netWifiCiphers_NONE == cipher) {
                continue;
            }
            this.group.addItem(MessageUtils.get(cipher.name()));
        }
        this.group.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });
    }

    private void initBgScanItems() {
        // Bgscan module
        this.labelBgscan.setText(MSGS.netWifiWirelessBgscanModule());
        this.bgscan.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.bgscan.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScan()));
            }
        });
        this.bgscan.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
            this.bgscan.addItem(MessageUtils.get(module.name()));
        }
        this.bgscan.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // BgScan RSSI threshold
        this.labelRssi.setText(MSGS.netWifiWirelessBgscanSignalStrengthThreshold());
        this.rssi.addMouseOverHandler(event -> {
            if (this.rssi.isEnabled()) {
                this.helpText.clear();
                this.helpText.add(new Span(MSGS.netWifiToolTipBgScanStrength()));
            }
        });

        // Bgscan short Interval
        this.labelShortI.setText(MSGS.netWifiWirelessBgscanShortInterval());
        this.shortI.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.shortI.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScanShortInterval()));
            }
        });
        this.shortI.addMouseOutHandler(event -> resetHelp());
        this.shortI.addValidator(newBgScanValidator(this.shortI));
        this.shortI.addChangeHandler(event -> this.longI.validate());

        // Bgscan long interval
        this.labelLongI.setText(MSGS.netWifiWirelessBgscanLongInterval());
        this.longI.addValidator(newBgScanValidator(this.longI));
        this.longI.addChangeHandler(event -> this.shortI.validate());
        this.longI.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.longI.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScanLongInterval()));
            }
        });
        this.longI.addMouseOutHandler(event -> resetHelp());
    }

    private void initAccessPointPingItems() {
        this.labelPing.setText(MSGS.netWifiWirelessPingAccessPoint());
        this.radio1.setText(MSGS.trueLabel());
        this.radio1.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio1.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio1.addChangeHandler(event -> setDirty(true));
        this.radio2.setText(MSGS.falseLabel());
        this.radio2.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio2.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());
        this.radio2.addChangeHandler(event -> setDirty(true));
    }

    private void initRegDomErrorModal() {
        this.regDomErrorModal.setTitle(MSGS.error());
        this.unavailableChannelErrorText.setText(MSGS.netWifiChannelMissingError());
        this.regDomErrorModal.addHideHandler(evt -> setDirty(true));
    }

    private List<GwtWifiHotspotEntry> getChannelFrequencyByIndex(int selectedIndex) {
        String selectedItem = this.channelList.getItemText(selectedIndex);

        GwtWifiHotspotEntry frequencyEntry = new GwtWifiHotspotEntry();

        if (selectedItem.equals(AUTOMATIC_CHANNEL_DESCRIPTION)) {
            frequencyEntry.setChannel(0);
            frequencyEntry.setFrequency(0);
        } else {
            String[] itemtext = selectedItem.split(" ");
            frequencyEntry.setChannel(Integer.parseInt(itemtext[1]));
            frequencyEntry.setFrequency(Integer.parseInt(itemtext[3]));
        }

        return Collections.singletonList(frequencyEntry);
    }

    private void remove8021xFromSecurityDropdown() {
        for (int i = 0; i < this.security.getItemCount(); i++) {
            if (this.security.getItemText(i).equals(WIFI_SECURITY_WPA2_WPA3_ENTERPRISE_MESSAGE)) {
                this.security.removeItem(i);
                return;
            }
        }
    }

    private void add8021xFromSecurityDropdown() {
        for (int i = 0; i < this.security.getItemCount(); i++) {
            if (this.security.getItemText(i).equals(WIFI_SECURITY_WPA2_WPA3_ENTERPRISE_MESSAGE)) {
                return;
            }
        }
        this.security.addItem(WIFI_SECURITY_WPA2_WPA3_ENTERPRISE_MESSAGE);
    }

    /*
     * BGScan validator
     */
    private Validator<String> newBgScanValidator(TextBox field) {
        return new Validator<String>() {

            @SuppressWarnings("all") // Ignoring warning, gwt doesn't support \\d in regex
            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (field.isVisible() && field.isEnabled()) {
                    try {
                        if (field.getText().trim().contains(".") || field.getText().trim().contains("-")
                                || !field.getText().trim().matches("[0-9]+")) {
                            result.add(new BasicEditorError(field, value, MSGS.netWifiBgScanInterval()));
                        } else if (Integer.parseInt(TabWirelessUi.this.shortI.getText().trim()) >= Integer
                                .parseInt(TabWirelessUi.this.longI.getText().trim())) {
                            result.add(new BasicEditorError(field, value, MSGS.netWifiBgScanIntervalValues()));
                        }
                    } catch (NumberFormatException e) {
                        result.add(new BasicEditorError(field, value, MSGS.deviceConfigError()));
                    }
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void loadCountryCode() {

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                TabWirelessUi.this.gwtNetworkService.getWifiCountryCode(token, new AsyncCallback<String>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        logger.info("getWifiCountryCode Failure");
                    }

                    @Override
                    public void onSuccess(String countryCode) {
                        TabWirelessUi.this.countryCode.setText(countryCode);
                        TabWirelessUi.this.activeConfig.setCountryCode(countryCode);

                        loadRadioMode();
                    }
                });
            }
        });

    }

    @SuppressWarnings("unchecked")
    private void setPasswordValidation() {

        EntryClassUi.loadPasswordStrengthRequirements(passwordStrengthRequirements -> {

            if (getWirelessMode() != GwtWifiWirelessMode.netWifiWirelessModeAccessPoint) {

                this.passwordInputForm.setInputPasswordValidators();
                checkPassword();
                return;

            }

            if (this.security != null && isWpaLikePairwiseSecurity()) {

                this.passwordInputForm.setInputPasswordValidatorsFrom(Optional.empty(), passwordStrengthRequirements);
                passwordStrengthRequirements.setPasswordMinimumLength(
                        Math.min(passwordStrengthRequirements.getPasswordMinimumLength(), 63));

                this.passwordInputForm.addInputPasswordValidator(
                        GwtValidators.regex(REGEX_PASS_WPA, MSGS.netWifiWirelessInvalidWPAPassword()));

            } else if (this.security != null
                    && this.security.getSelectedItemText().equals(WIFI_SECURITY_WEP_MESSAGE)) {

                this.passwordInputForm.setInputPasswordValidators();
                this.passwordInputForm
                        .addInputPasswordValidator(
                                GwtValidators.regex(REGEX_PASS_WEP, MSGS.netWifiWirelessInvalidWEPPassword()));
            } else {
                // Clears all validators when password is not required
                this.passwordInputForm.setInputPasswordValidators();
            }

            checkPassword();
        });
    }

    private void initWirelessScanModal() {
        this.ssidModal.setTitle("Wireless Networks");
        this.ssidTitle.setText("Available networks in range");
        this.ssidModal.show();

        this.searchingText.setText(MSGS.netWifiAlertScanning());
        this.noSsidText.setText(MSGS.netWifiAlertNoSSID());
        this.scanFailText.setText(MSGS.netWifiAlertScanFail());
    }

    private void initWirelessScanSsid() {
        this.ssidInit = true;
        TextColumn<GwtWifiHotspotEntry> col1 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return GwtSafeHtmlUtils.htmlUnescape(object.getSSID());
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col1, "SSID");
        this.ssidGrid.setColumnWidth(col1, "240px");

        TextColumn<GwtWifiHotspotEntry> col2 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getMacAddress();
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col2, "MAC Address");
        this.ssidGrid.setColumnWidth(col2, "140px");

        TextColumn<GwtWifiHotspotEntry> col3 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getSignalStrength());
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col3, "Signal Strength (dBm)");
        this.ssidGrid.setColumnWidth(col3, "70px");

        TextColumn<GwtWifiHotspotEntry> col4 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getChannel());
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col4, "Channel");
        this.ssidGrid.setColumnWidth(col4, "70px");

        TextColumn<GwtWifiHotspotEntry> col5 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getFrequency());
            }
        };
        col5.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col5, "Frequency");
        this.ssidGrid.setColumnWidth(col5, "70px");

        TextColumn<GwtWifiHotspotEntry> col6 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getSecurity();
            }
        };
        col6.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col6, "Security");
        this.ssidGrid.setColumnWidth(col6, "70px");
        this.ssidDataProvider.addDataDisplay(this.ssidGrid);

        this.ssidGrid.setSelectionModel(this.ssidSelectionModel);

        this.ssidSelectionModel.addSelectionChangeHandler(this::wirelessScanSsidSelectionHandler);
    }

    private void wirelessScanSsidSelectionHandler(SelectionChangeEvent event) {
        GwtWifiHotspotEntry wifiHotspotEntry = TabWirelessUi.this.ssidSelectionModel.getSelectedObject();
        if (wifiHotspotEntry != null) {
            int channelToSelect = getChannelIndexFromValue(wifiHotspotEntry.getChannel());

            if (channelToSelect < 0) {
                this.regDomErrorModal.show();
                logger.info("SSID Selected channel not in regdom: " + wifiHotspotEntry.getChannel());
            } else {
                this.channelList.setSelectedIndex(channelToSelect);
                this.activeConfig.setChannels(Collections.singletonList(wifiHotspotEntry.getChannel()));
            }

            getSecurityFromWirelessScanSelection(wifiHotspotEntry);

            getPairwiseCipherFromWirelessScanSelection(wifiHotspotEntry);

            getGroupCipherFromWirelessScanSelection(wifiHotspotEntry);

            TabWirelessUi.this.ssidModal.hide();

        }
    }

    private void getSecurityFromWirelessScanSelection(GwtWifiHotspotEntry wifiHotspotEntry) {
        TabWirelessUi.this.ssid.setValue(GwtSafeHtmlUtils.htmlUnescape(wifiHotspotEntry.getSSID()));
        String sec = wifiHotspotEntry.getSecurity();
        for (int i = 0; i < TabWirelessUi.this.security.getItemCount(); i++) {
            if (sec.equals(TabWirelessUi.this.security.getItemText(i))) {
                TabWirelessUi.this.security.setSelectedIndex(i);
                DomEvent.fireNativeEvent(Document.get().createChangeEvent(), TabWirelessUi.this.security);
                break;
            }
        }
    }

    private void getPairwiseCipherFromWirelessScanSelection(GwtWifiHotspotEntry wifiHotspotEntry) {
        String pairwiseCiphers = wifiHotspotEntry.getPairwiseCiphersEnum().name();
        for (int i = 0; i < TabWirelessUi.this.pairwise.getItemCount(); i++) {
            if (MessageUtils.get(pairwiseCiphers).equals(TabWirelessUi.this.pairwise.getItemText(i))) {
                TabWirelessUi.this.pairwise.setSelectedIndex(i);
                break;
            }
        }
    }

    private void getGroupCipherFromWirelessScanSelection(GwtWifiHotspotEntry wifiHotspotEntry) {
        String groupCiphers = wifiHotspotEntry.getGroupCiphersEnum().name();
        for (int i = 0; i < TabWirelessUi.this.group.getItemCount(); i++) {
            if (MessageUtils.get(groupCiphers).equals(TabWirelessUi.this.group.getItemText(i))) {
                TabWirelessUi.this.group.setSelectedIndex(i);
                break;
            }
        }
    }

    private void loadWirelessScanData(boolean recompute) {
        this.ssidDataProvider.getList().clear();
        this.searching.setVisible(true);
        this.noSsid.setVisible(false);
        this.ssidGrid.setVisible(false);
        this.scanFail.setVisible(false);
        if (this.selectedNetIfConfig != null) {
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    TabWirelessUi.this.gwtNetworkService.findWifiHotspots(token,
                            TabWirelessUi.this.selectedNetIfConfig.getName(),
                            TabWirelessUi.this.selectedNetIfConfig.getAccessPointWifiConfig().getWirelessSsid(),
                            recompute, new AsyncCallback<List<GwtWifiHotspotEntry>>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    TabWirelessUi.this.searching.setVisible(false);
                                    TabWirelessUi.this.noSsid.setVisible(false);
                                    TabWirelessUi.this.ssidGrid.setVisible(false);
                                    TabWirelessUi.this.scanFail.setVisible(true);
                                }

                                @Override
                                public void onSuccess(List<GwtWifiHotspotEntry> result) {
                                    fillWirelessScanDataOnSuccess(result);
                                }
                            });
                }
            });
        }
    }

    private void fillWirelessScanDataOnSuccess(List<GwtWifiHotspotEntry> wirelessScanResults) {
        TabWirelessUi.this.ssidDataProvider.getList().clear();
        for (GwtWifiHotspotEntry pair : wirelessScanResults) {
            if (pair.getSecurity().equals(GwtWifiSecurity.netWifiSecurityWPA3.value())
                    || pair.getSecurity()
                            .equals(GwtWifiSecurity.netWifiSecurityWPA2_WPA3.value())) {
                if (TabWirelessUi.this.isWPA3Supported.get()) {
                    TabWirelessUi.this.ssidDataProvider.getList().add(pair);
                }
            } else {
                TabWirelessUi.this.ssidDataProvider.getList().add(pair);
            }
        }
        TabWirelessUi.this.ssidDataProvider.flush();
        if (!TabWirelessUi.this.ssidDataProvider.getList().isEmpty()) {
            TabWirelessUi.this.searching.setVisible(false);
            TabWirelessUi.this.noSsid.setVisible(false);
            int size = TabWirelessUi.this.ssidDataProvider.getList().size();
            TabWirelessUi.this.ssidGrid.setVisibleRange(0, size);
            TabWirelessUi.this.ssidGrid.setVisible(true);
            TabWirelessUi.this.scanFail.setVisible(false);
        } else {
            TabWirelessUi.this.searching.setVisible(false);
            TabWirelessUi.this.noSsid.setVisible(true);
            TabWirelessUi.this.ssidGrid.setVisible(false);
            TabWirelessUi.this.scanFail.setVisible(false);
        }
    }

    private int getChannelIndexFromValue(int channelValue) {

        if (channelValue == 0) {
            return 0;
        }

        for (int i = 0; i < this.channelList.getItemCount(); i++) {
            String[] values = this.channelList.getItemText(i).split(" ");

            try {
                int channel = Integer.parseInt(values[1]);
                if (channel == channelValue) {
                    return i;
                }
            } catch (NumberFormatException automaticChannel) {
                // skip non-integer values to avoid ui crash
            }
        }
        return -1;
    }

    private int getChannelValueByIndex(int index) {
        String itemText = this.channelList.getItemText(index);

        if (itemText.equalsIgnoreCase(AUTOMATIC_CHANNEL_DESCRIPTION)) {
            return 0;
        }

        String[] values = itemText.split(" ");
        return Integer.parseInt(values[1]);
    }

    private GwtWifiConfig getGwtWifiConfig() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        getWirelessModeFromConfig(gwtWifiConfig);
        getSsidFromConfig(gwtWifiConfig);
        getRadioModeFromConfig(gwtWifiConfig);
        getSecurityFromConfig(gwtWifiConfig);
        getGroupCiphersFromConfig(gwtWifiConfig);
        getBgscanFromConfig(gwtWifiConfig);

        // ping access point
        gwtWifiConfig.setPingAccessPoint(this.radio1.getValue());

        // ignore SSID
        gwtWifiConfig.setIgnoreSSID(this.radio3.getValue());

        // Country Code is not editable at moment.
        gwtWifiConfig.setCountryCode(this.countryCode.getValue());

        return gwtWifiConfig;
    }

    private void getWirelessModeFromConfig(GwtWifiConfig gwtWifiConfig) {
        // Wireless Mode
        GwtWifiWirelessMode wifiMode;
        if (this.wireless.getSelectedItemText().equals(MessageUtils.get(WIFI_MODE_STATION))) {
            wifiMode = GwtWifiWirelessMode.netWifiWirelessModeStation;
        } else {
            wifiMode = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint;
        }
        gwtWifiConfig.setWirelessMode(wifiMode.name());

        // Driver
        String driver = "";
        switch (wifiMode) {
            case netWifiWirelessModeAccessPoint: {
                driver = this.selectedNetIfConfig.getAccessPointWifiConfig().getDriver();
                break;
            }

            case netWifiWirelessModeAdHoc: {
                driver = this.selectedNetIfConfig.getAdhocWifiConfig().getDriver();
                break;
            }

            case netWifiWirelessModeStation: {
                driver = this.selectedNetIfConfig.getStationWifiConfig().getDriver();
                break;
            }

            default:
                break;
        }

        gwtWifiConfig.setDriver(driver); // use previous value
    }

    private void getSsidFromConfig(GwtWifiConfig gwtWifiConfig) {
        gwtWifiConfig.setWirelessSsid(GwtSafeHtmlUtils.htmlUnescape(this.ssid.getText().trim()));
    }

    private void getRadioModeFromConfig(GwtWifiConfig gwtWifiConfig) {
        String radioValue = this.radio.getSelectedValue();

        gwtWifiConfig.setRadioMode(radioValueToRadioMode(radioValue).name());

        gwtWifiConfig
                .setChannels(Collections.singletonList(getChannelValueByIndex(this.channelList.getSelectedIndex())));
    }

    private void getSecurityFromConfig(GwtWifiConfig gwtWifiConfig) {
        // Security Type
        String secValue = this.security.getSelectedItemText();
        for (GwtWifiSecurity sec : GwtWifiSecurity.values()) {
            if (MessageUtils.get(sec.name()).equals(secValue)) {
                gwtWifiConfig.setSecurity(sec.name());
            }
        }

        // Pairwise Ciphers
        String pairWiseCiphersValue = this.pairwise.getSelectedItemText();
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
            if (MessageUtils.get(ciphers.name()).equals(pairWiseCiphersValue)) {
                gwtWifiConfig.setPairwiseCiphers(ciphers.name());
            }
        }

        // Password
        if (this.groupPassword.getValidationState().equals(ValidationState.NONE)) {
            gwtWifiConfig.setPassword(this.passwordInputForm.getInputPasswordText());
        }
    }

    private void getGroupCiphersFromConfig(GwtWifiConfig gwtWifiConfig) {
        String groupCiphersValue = this.group.getSelectedItemText();
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
            if (MessageUtils.get(ciphers.name()).equals(groupCiphersValue)) {
                gwtWifiConfig.setGroupCiphers(ciphers.name());
            }
        }
    }

    private void getBgscanFromConfig(GwtWifiConfig gwtWifiConfig) {
        String bgscanModuleValue = this.bgscan.getSelectedItemText();
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
            if (MessageUtils.get(module.name()).equals(bgscanModuleValue)) {
                gwtWifiConfig.setBgscanModule(module.name());
            }
        }

        String bgscanRssiThreshold = this.rssi.getText().trim();
        if (!bgscanRssiThreshold.isEmpty()) {
            gwtWifiConfig.setBgscanRssiThreshold(Integer.parseInt(bgscanRssiThreshold));
        } else {
            gwtWifiConfig.setBgscanRssiThreshold(0);
        }
        String bgscanShortInterval = this.shortI.getText().trim();
        if (!bgscanShortInterval.isEmpty()) {
            gwtWifiConfig.setBgscanShortInterval(Integer.parseInt(bgscanShortInterval));
        } else {
            gwtWifiConfig.setBgscanShortInterval(0);
        }
        String bgscanLongInterval = this.longI.getText().trim();
        if (!bgscanLongInterval.isEmpty()) {
            gwtWifiConfig.setBgscanLongInterval(Integer.parseInt(bgscanLongInterval));
        } else {
            gwtWifiConfig.setBgscanLongInterval(0);
        }
    }

    private GwtWifiRadioMode radioValueToRadioMode(String radioValue) {

        for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
            String modeName = mode.name();
            if (modeName.equals(radioValue)) {
                return mode;
            }
        }

        return GwtWifiRadioMode.netWifiRadioModeBGN;
    }

    private boolean checkPassword() {
        boolean result = true;

        if (this.passwordInputForm != null) {
            this.groupPassword.setValidationState(ValidationState.NONE);
        }
        if (this.passwordInputForm != null && this.passwordInputForm.isInputPasswordVisible()
                && this.passwordInputForm.isInputPasswordEnabled()
                && !this.passwordInputForm.validateInputPassword()) {
            this.groupPassword.setValidationState(ValidationState.ERROR);
            result = false;
        } else {
            this.groupPassword.setValidationState(ValidationState.NONE);
        }
        return result;
    }

    private void updateChannelListValues(List<GwtWifiChannelFrequency> freqChannels) {

        int selectedChannelValue = 0;

        if (TabWirelessUi.this.channelList.getItemCount() != 0) {
            selectedChannelValue = this.channelList.getSelectedIndex();
        }

        this.channelList.clear();
        addAutomaticChannel(freqChannels);

        freqChannels.stream().forEach(this::addItemChannelList);

        if (this.activeConfig != null && this.activeConfig.getChannels() != null
                && !this.activeConfig.getChannels().isEmpty()) {
            this.netTabs.hardwareTab.channel
                    .setText(this.channelList.getItemText(selectedChannelValue));

            this.channelList.setSelectedIndex(selectedChannelValue);
        }

        this.noChannels.setVisible(this.channelList.getItemCount() <= 0);
    }

    private void addItemChannelList(GwtWifiChannelFrequency channelFrequency) {

        if (channelFrequency.getChannel() == 0 && channelFrequency.getFrequency() == 0) {
            this.channelList.addItem(AUTOMATIC_CHANNEL_DESCRIPTION);
            return;
        }

        String frequencyString = channelFrequency.getFrequency() > -1 ? " - " + channelFrequency.getFrequency() + " MHz"
                : "";

        this.channelList.addItem("Channel " + channelFrequency.getChannel() + frequencyString);

        if (shouldBeDisabled(channelFrequency)) {
            disableLastChannelItem();
        }
    }

    private boolean shouldBeDisabled(GwtWifiChannelFrequency channelFrequency) {
        boolean isNotValidCountryCode = this.countryCode.getValue().isEmpty()
                || this.countryCode.getValue().equals(WORLD_REGION_COUNTRY_CODE);

        return channelFrequency.isDisabled()
                || channelFrequency.isNoIrradiation() && !channelFrequency.isRadarDetection()
                || channelFrequency.isNoIrradiation() && channelFrequency.isRadarDetection() && isNotValidCountryCode;
    }

    private void disableLastChannelItem() {

        NodeList<Element> channelElements = this.channelList.getElement().getElementsByTagName("option");

        int channelLength = channelElements.getLength();
        Element element = channelElements.getItem(channelLength - 1);

        element.setAttribute("disabled", "disabled");
        element.addClassName("list-group-item disabled");
    }

    private void addAutomaticChannel(List<GwtWifiChannelFrequency> freqs) {
        if (!containsChannel0(freqs)) {
            GwtWifiChannelFrequency automaticChannel = new GwtWifiChannelFrequency(0, 0);
            freqs.add(0, automaticChannel);
        }
    }

    private boolean containsChannel0(List<GwtWifiChannelFrequency> freqs) {
        for (GwtWifiChannelFrequency freq : freqs) {
            if (freq.getChannel() == 0) {
                return true;
            }
        }

        return false;
    }

    private void loadRadioMode() {

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {

                TabWirelessUi.this.gwtNetworkService.isIEEE80211ACSupported(token,
                        TabWirelessUi.this.selectedNetIfConfig.getName(), new AsyncCallback<Boolean>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                logger.info("Unable to read 802.11ac capability");
                            }

                            @Override
                            public void onSuccess(Boolean acSupported) {

                                String selectedRadioMode = TabWirelessUi.this.radio.getSelectedValue() != null
                                        ? TabWirelessUi.this.radio.getSelectedValue()
                                        : null;

                                TabWirelessUi.this.radio.clear();

                                fillRadioMode(acSupported);

                                if (selectedRadioMode != null) {
                                    setRadioModeByValue(selectedRadioMode);
                                } else if (TabWirelessUi.this.activeConfig != null) {
                                    setRadioModeByValue(TabWirelessUi.this.activeConfig.getRadioMode());
                                }

                                loadChannelFrequencies();
                            }
                        });
            }

        });
    }

    private void loadChannelFrequencies() {
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                GwtWifiRadioMode radioMode = radioValueToRadioMode(TabWirelessUi.this.radio.getSelectedValue());

                TabWirelessUi.this.gwtNetworkService.findFrequencies(token,
                        TabWirelessUi.this.selectedNetIfConfig.getName(), radioMode,
                        new AsyncCallback<List<GwtWifiChannelFrequency>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                logger.info("findFrequencies Failure");
                            }

                            @Override
                            public void onSuccess(List<GwtWifiChannelFrequency> freqChannels) {
                                updateChannelListValues(freqChannels);

                            }
                        });
            }
        });

    }

    private void fillRadioMode(boolean acSupported) {
        this.radio.addItem(WIFI_BAND_2GHZ_MESSAGE, WIFI_RADIO_BG);
        this.radio.addItem(WIFI_BAND_5GHZ_MESSAGE, WIFI_RADIO_A);
        this.radio.addItem(WIFI_BAND_BOTH_MESSAGE, WIFI_RADIO_BGN);
    }

    private void configureWifiSecurityListBox() {

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                TabWirelessUi.this.gwtDeviceService.findSystemProperties(token,
                        new AsyncCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                logger.info("Unable to read WPA3 WiFi Security support property.");
                            }

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                Optional<GwtGroupedNVPair> wpa3SupportPair = result.stream().filter(
                                        pair -> pair.getName().equals(SystemService.KEY_WPA3_WIFI_SECURITY_ENABLE))
                                        .findFirst();
                                if (wpa3SupportPair.isPresent()
                                        && Boolean.parseBoolean(wpa3SupportPair.get().getValue())) {
                                    TabWirelessUi.this.isWPA3Supported.set(true);
                                    initWifiSecurityListBoxItem();
                                }
                            }
                        });
            }
        });
    }

    /*
     * Utilities
     */

    /*
     * Check if selected security is WPA like (WPA, WPA2, WPA3, WPA/WPA2, WPA2/WPA3)
     */
    private boolean isWpaLikePairwiseSecurity() {
        return this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA_MESSAGE)
                || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_MESSAGE)
                || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA_WPA2_MESSAGE)
                || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA3_MESSAGE)
                || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_WPA3_MESSAGE);
    }

    /*
     * Compose tooltip message for Security label
     */

    private String composeNetWifiToolTipSecurity(boolean isWPA3WifiSecuritySupported) {
        String toolTipMessage = MSGS.netWifiToolTipSecurity();
        if (isWPA3WifiSecuritySupported) {
            toolTipMessage += "<br><br>" + MSGS.netWifiToolTipSecurityWPA3();
        }
        return toolTipMessage;
    }

}
