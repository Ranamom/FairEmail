package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2021 by Marcel Bokhorst (M66B)
*/

import static eu.faircode.email.ServiceAuthenticator.AUTH_TYPE_PASSWORD;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import javax.mail.AuthenticationFailedException;

public class FragmentQuickSetup extends FragmentBase {
    private ViewGroup view;
    private ScrollView scroll;

    private TextView tvPrivacy;
    private EditText etName;
    private EditText etEmail;
    private TextInputLayout tilPassword;
    private TextView tvCharacters;
    private Button btnCheck;
    private ContentLoadingProgressBar pbCheck;
    private TextView tvPatience;

    private TextView tvError;
    private TextView tvErrorHint;
    private TextView tvInstructions;
    private Button btnHelp;
    private Button btnSupport;

    private TextView tvImap;
    private TextView tvImapFingerprint;
    private TextView tvSmtp;
    private TextView tvSmtpFingerprint;
    private Button btnSave;
    private ContentLoadingProgressBar pbSave;

    private Group grpSetup;
    private Group grpError;

    private static final String PRIVACY_URI = "https://www.mozilla.org/privacy/";

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSubtitle(R.string.title_setup_other);
        setHasOptionsMenu(true);

        view = (ViewGroup) inflater.inflate(R.layout.fragment_quick_setup, container, false);
        scroll = view.findViewById(R.id.scroll);

        // Get controls
        tvPrivacy = view.findViewById(R.id.tvPrivacy);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tvCharacters = view.findViewById(R.id.tvCharacters);
        btnCheck = view.findViewById(R.id.btnCheck);
        pbCheck = view.findViewById(R.id.pbCheck);
        tvPatience = view.findViewById(R.id.tvPatience);

        tvError = view.findViewById(R.id.tvError);
        tvErrorHint = view.findViewById(R.id.tvErrorHint);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        btnHelp = view.findViewById(R.id.btnHelp);
        btnSupport = view.findViewById(R.id.btnSupport);

        tvImap = view.findViewById(R.id.tvImap);
        tvImapFingerprint = view.findViewById(R.id.tvImapFingerprint);
        tvSmtp = view.findViewById(R.id.tvSmtp);
        tvSmtpFingerprint = view.findViewById(R.id.tvSmtpFingerprint);
        btnSave = view.findViewById(R.id.btnSave);
        pbSave = view.findViewById(R.id.pbSave);

        grpSetup = view.findViewById(R.id.grpSetup);
        grpError = view.findViewById(R.id.grpError);

        // Wire controls

        tvPrivacy.setPaintFlags(tvPrivacy.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse(PRIVACY_URI), false);
            }
        });

        tilPassword.setHintEnabled(false);

        tilPassword.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    onSave(true);
                    return true;
                }
                return false;
            }
        });

        tilPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                boolean warning = (Helper.containsWhiteSpace(password) ||
                        Helper.containsControlChars(password));
                tvCharacters.setVisibility(warning &&
                        tilPassword.getVisibility() == View.VISIBLE
                        ? View.VISIBLE : View.GONE);
            }
        });

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave(true);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave(false);
            }
        });

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, (Uri) btnHelp.getTag());
                Helper.view(getContext(), intent);
            }
        });

        btnSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Helper.getSupportUri(v.getContext()), false);
            }
        });

        // Initialize
        tvCharacters.setVisibility(View.GONE);
        tvImapFingerprint.setText(null);
        tvSmtpFingerprint.setText(null);
        pbCheck.setVisibility(View.GONE);
        tvPatience.setVisibility(View.GONE);
        pbSave.setVisibility(View.GONE);
        tvInstructions.setVisibility(View.GONE);
        tvInstructions.setMovementMethod(LinkMovementMethod.getInstance());
        btnHelp.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        grpSetup.setVisibility(View.GONE);
        grpError.setVisibility(View.GONE);

        return view;
    }

    private void onSave(boolean check) {
        Bundle args = new Bundle();
        args.putString("name", etName.getText().toString().trim());
        args.putString("email", etEmail.getText().toString().trim());
        args.putString("password", tilPassword.getEditText().getText().toString());
        args.putString("imap_fingerprint", tvImapFingerprint.getText().toString());
        args.putString("smtp_fingerprint", tvSmtpFingerprint.getText().toString());
        args.putBoolean("check", check);

        new SimpleTask<EmailProvider>() {
            @Override
            protected void onPreExecute(Bundle args) {
                boolean check = args.getBoolean("check");

                Helper.setViewsEnabled(view, false);
                pbCheck.setVisibility(check ? View.VISIBLE : View.GONE);
                tvPatience.setVisibility(check ? View.VISIBLE : View.GONE);
                pbSave.setVisibility(check ? View.GONE : View.VISIBLE);
                grpError.setVisibility(View.GONE);
                tvInstructions.setVisibility(View.GONE);
                btnHelp.setVisibility(View.GONE);
                btnSave.setVisibility(check ? View.GONE : View.VISIBLE);
                grpSetup.setVisibility(check ? View.GONE : View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                Helper.setViewsEnabled(view, true);
                pbCheck.setVisibility(View.GONE);
                tvPatience.setVisibility(View.GONE);
                pbSave.setVisibility(View.GONE);
            }

            @Override
            protected EmailProvider onExecute(Context context, Bundle args) throws Throwable {
                String name = args.getString("name");
                String email = args.getString("email");
                String password = args.getString("password");
                boolean check = args.getBoolean("check");
                String imap_fingerprint = args.getString("imap_fingerprint");
                String smtp_fingerprint = args.getString("smtp_fingerprint");

                if (TextUtils.isEmpty(name))
                    throw new IllegalArgumentException(context.getString(R.string.title_no_name));
                if (TextUtils.isEmpty(email))
                    throw new IllegalArgumentException(context.getString(R.string.title_no_email));
                if (!Helper.EMAIL_ADDRESS.matcher(email).matches())
                    throw new IllegalArgumentException(context.getString(R.string.title_email_invalid, email));
                if (TextUtils.isEmpty(password))
                    throw new IllegalArgumentException(context.getString(R.string.title_no_password));
                if (TextUtils.isEmpty(imap_fingerprint))
                    imap_fingerprint = null;
                if (TextUtils.isEmpty(smtp_fingerprint))
                    smtp_fingerprint = null;

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ani = (cm == null ? null : cm.getActiveNetworkInfo());
                if (ani == null || !ani.isConnected())
                    throw new IllegalArgumentException(context.getString(R.string.title_no_internet));

                EmailProvider provider = EmailProvider.fromEmail(context, email, EmailProvider.Discover.ALL);
                args.putParcelable("provider", provider);

                int at = email.indexOf('@');
                String username = email.substring(0, at);

                String user = (provider.user == EmailProvider.UserType.EMAIL ? email : username);
                Log.i("User type=" + provider.user + " name=" + user);

                List<EntityFolder> folders;

                String aprotocol = (provider.imap.starttls ? "imap" : "imaps");
                int aencryption = (provider.imap.starttls ? EmailService.ENCRYPTION_STARTTLS : EmailService.ENCRYPTION_SSL);
                try (EmailService iservice = new EmailService(
                        context, aprotocol, null, aencryption, false, EmailService.PURPOSE_CHECK, true)) {
                    try {
                        iservice.connect(
                                provider.imap.host, provider.imap.port,
                                AUTH_TYPE_PASSWORD, null,
                                user, password,
                                null, imap_fingerprint);
                    } catch (EmailService.UntrustedException ex) {
                        if (check) {
                            imap_fingerprint = ex.getFingerprint();
                            iservice.connect(
                                    provider.imap.host, provider.imap.port,
                                    AUTH_TYPE_PASSWORD, null,
                                    user, password,
                                    null, imap_fingerprint);
                        } else
                            throw ex;
                    } catch (Throwable ex) {
                        Log.w(ex);
                        // Why not AuthenticationFailedException?
                        // Some providers terminate the connection with an invalid username
                        if (user.equals(username))
                            throw ex;
                        else
                            try {
                                user = username;
                                Log.i("Retry with user=" + user);
                                iservice.connect(
                                        provider.imap.host, provider.imap.port,
                                        AUTH_TYPE_PASSWORD, null,
                                        user, password,
                                        null, null);
                            } catch (Throwable ex1) {
                                Log.w(ex1);
                                if (!(ex instanceof AuthenticationFailedException) &&
                                        ex1 instanceof AuthenticationFailedException)
                                    throw ex1;
                                else
                                    throw ex;
                            }
                    }

                    folders = iservice.getFolders();
                }

                Long max_size = null;
                String iprotocol = (provider.smtp.starttls ? "smtp" : "smtps");
                int iencryption = (provider.smtp.starttls ? EmailService.ENCRYPTION_STARTTLS : EmailService.ENCRYPTION_SSL);
                try (EmailService iservice = new EmailService(
                        context, iprotocol, null, iencryption, false,
                        EmailService.PURPOSE_CHECK, true)) {
                    iservice.setUseIp(provider.useip, null);
                    iservice.connect(
                            provider.smtp.host, provider.smtp.port,
                            AUTH_TYPE_PASSWORD, null,
                            user, password,
                            null, smtp_fingerprint);
                    max_size = iservice.getMaxSize();
                } catch (EmailService.UntrustedException ex) {
                    if (check)
                        smtp_fingerprint = ex.getFingerprint();
                    else
                        throw ex;
                }

                if (check) {
                    args.putString("imap_fingerprint", imap_fingerprint);
                    args.putString("smtp_fingerprint", smtp_fingerprint);
                    return provider;
                }

                DB db = DB.getInstance(context);
                try {
                    db.beginTransaction();

                    EntityAccount primary = db.account().getPrimaryAccount();

                    // Create account
                    EntityAccount account = new EntityAccount();

                    account.host = provider.imap.host;
                    account.encryption = aencryption;
                    account.port = provider.imap.port;
                    account.auth_type = AUTH_TYPE_PASSWORD;
                    account.user = user;
                    account.password = password;
                    account.fingerprint = imap_fingerprint;

                    account.name = provider.name + "/" + username;

                    account.synchronize = true;
                    account.primary = (primary == null);

                    if (provider.keepalive > 0)
                        account.poll_interval = provider.keepalive;

                    account.partial_fetch = provider.partial;

                    account.created = new Date().getTime();
                    account.last_connected = account.created;

                    account.id = db.account().insertAccount(account);
                    args.putLong("account", account.id);
                    EntityLog.log(context, "Quick added account=" + account.name);

                    // Create folders
                    for (EntityFolder folder : folders) {
                        EntityFolder existing = db.folder().getFolderByName(account.id, folder.name);
                        if (existing == null) {
                            folder.account = account.id;
                            folder.setSpecials(account);
                            folder.id = db.folder().insertFolder(folder);
                            EntityLog.log(context, "Quick added folder=" + folder.name + " type=" + folder.type);
                            if (folder.synchronize)
                                EntityOperation.sync(context, folder.id, true);
                        }
                    }

                    // Set swipe left/right folder
                    for (EntityFolder folder : folders)
                        if (EntityFolder.TRASH.equals(folder.type))
                            account.swipe_left = folder.id;
                        else if (EntityFolder.ARCHIVE.equals(folder.type))
                            account.swipe_right = folder.id;

                    db.account().updateAccount(account);

                    // Create identity
                    EntityIdentity identity = new EntityIdentity();
                    identity.name = name;
                    identity.email = email;
                    identity.account = account.id;

                    identity.host = provider.smtp.host;
                    identity.encryption = iencryption;
                    identity.port = provider.smtp.port;
                    identity.auth_type = AUTH_TYPE_PASSWORD;
                    identity.user = user;
                    identity.password = password;
                    identity.fingerprint = smtp_fingerprint;
                    identity.use_ip = provider.useip;
                    identity.synchronize = true;
                    identity.primary = true;
                    identity.max_size = max_size;

                    identity.id = db.identity().insertIdentity(identity);
                    EntityLog.log(context, "Quick added identity=" + identity.name + " email=" + identity.email);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                ServiceSynchronize.eval(context, "quick setup");

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, EmailProvider result) {
                setManual(false);

                boolean check = args.getBoolean("check");
                if (check) {
                    tvImap.setText(result == null ? null : result.imap.toString());
                    tvImapFingerprint.setText(args.getString("imap_fingerprint"));
                    tvSmtp.setText(result == null ? null : result.smtp.toString());
                    tvSmtpFingerprint.setText(args.getString("smtp_fingerprint"));
                    btnSave.setVisibility(result == null ? View.GONE : View.VISIBLE);
                    grpSetup.setVisibility(result == null ? View.GONE : View.VISIBLE);
                } else {
                    FragmentDialogAccount fragment = new FragmentDialogAccount();
                    fragment.setArguments(args);
                    fragment.setTargetFragment(FragmentQuickSetup.this, ActivitySetup.REQUEST_DONE);
                    fragment.show(getParentFragmentManager(), "quick:review");
                }
            }

            @Override
            protected void onException(final Bundle args, Throwable ex) {
                Log.e(ex);
                setManual(true);
                EmailProvider provider = args.getParcelable("provider");

                etName.clearFocus();
                etEmail.clearFocus();
                Helper.hideKeyboard(view);

                if (ex instanceof AuthenticationFailedException) {
                    String message = getString(R.string.title_setup_no_auth_hint);
                    if (provider != null && provider.appPassword)
                        message += "\n\n" + getString(R.string.title_setup_app_password_hint);
                    tvErrorHint.setText(message);
                } else
                    tvErrorHint.setText(R.string.title_setup_no_settings_hint);

                if (ex instanceof IllegalArgumentException || ex instanceof UnknownHostException) {
                    tvError.setText(ex.getMessage());
                    grpError.setVisibility(View.VISIBLE);

                    getMainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            scroll.smoothScrollTo(0, btnSupport.getBottom());
                        }
                    });
                } else {
                    tvError.setText(Log.formatThrowable(ex, false));
                    grpError.setVisibility(View.VISIBLE);

                    if (provider != null && provider.link != null) {
                        Uri uri = Uri.parse(provider.link);
                        btnHelp.setTag(uri);
                        btnHelp.setVisibility(View.VISIBLE);
                    }

                    if (provider != null && provider.documentation != null) {
                        tvInstructions.setText(HtmlHelper.fromHtml(provider.documentation.toString(), getContext()));
                        tvInstructions.setVisibility(View.VISIBLE);
                    }

                    if (provider != null &&
                            provider.imap != null && provider.smtp != null) {
                        tvImap.setText(provider.imap.toString());
                        tvImapFingerprint.setText(null);
                        tvSmtp.setText(provider.smtp.toString());
                        tvSmtpFingerprint.setText(null);
                        grpSetup.setVisibility(View.VISIBLE);
                    }

                    getMainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            scroll.smoothScrollTo(0, btnSupport.getBottom());
                        }
                    });
                }
            }

            private void setManual(boolean manual) {
                FragmentActivity activity = getActivity();
                if (activity == null)
                    return;

                Intent intent = activity.getIntent();
                if (intent == null)
                    return;

                intent.putExtra("manual", manual);
            }
        }.execute(this, args, "setup:quick");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case ActivitySetup.REQUEST_DONE:
                    finish();
                    break;
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }
}
