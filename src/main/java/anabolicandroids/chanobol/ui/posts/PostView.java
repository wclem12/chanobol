package anabolicandroids.chanobol.ui.posts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.builder.AnimateGifMode;
import com.koushikdutta.ion.builder.Builders;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.images.ImgIdExt;
import anabolicandroids.chanobol.util.MaxWidthCardView;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class PostView extends MaxWidthCardView {
    @InjectView(R.id.header) ViewGroup header;
    @InjectView(R.id.number) TextView number;
    @InjectView(R.id.date) TextView date;
    @InjectView(R.id.repliesContainer) ViewGroup repliesContainer;
    @InjectView(R.id.replies) TextView replies;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.play) ImageView play;
    @InjectView(R.id.progressbar) ProgressBar progress;
    @InjectView(R.id.text) TextView text;
    @InjectView(R.id.footer) ViewGroup footer;
    @InjectView(R.id.footerTripCode) TextView footerTripCode;
    @InjectView(R.id.footerFlag) ImageView footerFlag;
    @InjectView(R.id.footerCountryName) TextView footerCountryName;
    @InjectView(R.id.footerImage) TextView footerImage;

    private static final int W = 0, H = 1;
    private int maxImgWidth;
    private int maxImgHeight;

    public Post post;

    public PostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        // TODO: Is onFinishInflate called on a configuration change if the containing fragment's instance is retained?
        // If not these values should be recalculated.
        int screenWidth = Util.getScreenWidth(getContext());
        int screenHeight = Util.getScreenHeight(getContext());
        maxImgWidth = screenWidth;
        maxImgHeight = (int) (screenHeight * 0.85);
    }

    private void reset() {
        repliesContainer.setVisibility(View.GONE);
        header.setOnClickListener(null);
        image.setVisibility(View.GONE);
        image.setImageBitmap(null);
        image.setOnClickListener(null);
        play.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        footerTripCode.setVisibility(View.GONE);
        footerFlag.setVisibility(View.GONE);
        footerFlag.setImageBitmap(null);
        footerCountryName.setVisibility(View.GONE);
        footerImage.setVisibility(View.GONE);
    }

    // If needed reduce the image size such that it is only as big as needed and is
    // never bigger than approx. the screen size. For example, an image with a height
    // of one billion pixels should fit on the screen. Therefore, the width and height
    // have to be reduced proportionally such that the new height is smaller equal the
    // screen height.
    private void calcSize(int[] size, Post post) {
        double w = post.imageWidth, h = post.imageHeight;
        if (w >= maxImgWidth) {
            double w_old = w;
            w = maxImgWidth;
            h *= w / w_old;
        }
        if (h >= maxImgHeight) {
            double h_old = h;
            h = maxImgHeight;
            w *= h / h_old;
        }
        size[W] = (int) w;
        size[H] = (int) h;
    }

    private void initImageCallback(final ImgIdExt imgIdExt, final Bitmap b, final boolean thumbnail,
                                   final PostsFragment.ImageCallback cb) {
        image.setOnClickListener(new OnClickListener() {
            Drawable cached;
            @Override public void onClick(View v) {
                if (cached == null && b != null) {
                    cached = new BitmapDrawable(getResources(), b.copy(b.getConfig(), true));
                }
                cb.onClick(imgIdExt, cached, image, thumbnail);
            }
        });
    }

    private void initText(final Ion ion,
                          final Post post,
                          final PostsFragment.RepliesCallback repliesCallback,
                          final PostsFragment.ReferencedPostCallback referencedPostCallback) {
        number.setText(post.number);
        date.setText(DateUtils.getRelativeTimeSpanString(
                post.time * 1000L, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        if (post.replyCount != 0) {
            repliesContainer.setVisibility(View.VISIBLE);
            replies.setText(post.replyCount + "r");
            header.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (repliesCallback != null) repliesCallback.onClick(post);
                }
            });
        }

        String s = post.subject;
        if (s == null) s = "";
        else s = "<h2>" + s + "</h2>";
        String t = post.text;
        if (t == null) t = "";

        // TODO: I'd love to use a more principled approach
        String r = (s + t)
                .replaceAll("<wbr>", "")
                // TODO: I simply want to turn all plain text urls into links without matching urls that are assigned to a href attribute
                .replaceAll("(?!href=\")("+Patterns.WEB_URL.pattern()+")", "<a href=\"$1\">$1</a>")
                .replaceAll("<span class=\"quote\">", "<font color=\"#23b423\">")
                .replaceAll("</span>", "</font>");
        setTextViewHTML(post.number, text, r, referencedPostCallback);

        if (post.imageId != null && !"null".equals(post.imageId)) {
            footer.setVisibility(VISIBLE);
            footerImage.setVisibility(VISIBLE);
            footerImage.setText(String.format("%dx%d ~ %s ~ %s%s", post.imageWidth, post.imageHeight,
                    Util.readableFileSize(post.filesize), post.filename, post.imageExtension));
        }
        if (post.tripCode != null) {
            footer.setVisibility(VISIBLE);
            footerTripCode.setVisibility(VISIBLE);
            footerTripCode.setText(post.tripCode);
        }
        if (post.country != null) {
            footer.setVisibility(VISIBLE);
            footerCountryName.setVisibility(VISIBLE);
            footerFlag.setVisibility(VISIBLE);
            footerCountryName.setText(post.countryName);
            ion.build(footerFlag)
                    .fitCenter()
                    .load("file:///android_asset/flags/"+post.country.toLowerCase()+".png");
        }
    }

    // http://stackoverflow.com/a/19989677/283607
    protected void setTextViewHTML(final String quoterId, TextView text, String html,
                                   final PostsFragment.ReferencedPostCallback referencedPostCallback) {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(final URLSpan span : urls) {
            if (!span.getURL().startsWith("#p")) continue;
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    if (referencedPostCallback != null)
                        referencedPostCallback.onClick(quoterId, span.getURL().substring(2));
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
        text.setText(strBuilder);
        text.setLinksClickable(true);
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // The raison d'être for this method is to immediately populate the OP post cardview
    // when viewing a single thread without first waiting for the 4Chan API request to finish
    // that gets all the posts in order to give the impression (illusion) of promptness
    // (compare with the iOS splash screen concept).
    public void bindToOp(final Drawable opImage,
                         final Post post, final String boardName,
                         final Ion ion) {
        this.post = post;
        reset();
        initText(ion, post, null, null);

        final int[] size = new int[2]; calcSize(size, post);
        image.setVisibility(View.VISIBLE);
        image.setImageDrawable(opImage);
        image.getLayoutParams().height = size[H];
        image.requestLayout();
        ion.build(getContext()).load(ApiModule.imgUrl(boardName, post.imageId, post.imageExtension)).asBitmap().tryGet();
    }

    public void bindTo(final Post post, final String boardName,
                       final String threadId, final Ion ion,
                       final PostsFragment.RepliesCallback repliesCallback,
                       final PostsFragment.ReferencedPostCallback referencedPostCallback,
                       final PostsFragment.ImageCallback imageCallback) {
        this.post = post;
        reset();
        initText(ion, post, repliesCallback, referencedPostCallback);

        if (post.imageId != null && !"null".equals(post.imageId)) {

            final int[] size = new int[2]; calcSize(size, post);
            progress.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
            image.getLayoutParams().height = size[H];
            String thumbUrl = ApiModule.thumbUrl(boardName, post.imageId);
            ion.build(image)
                .load(thumbUrl)
                .withBitmapInfo()
                .setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                    @Override
                    public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                        BitmapInfo bitmapInfo = result.getBitmapInfo();
                        if (e != null || bitmapInfo == null) {
                            progress.setVisibility(View.GONE);
                            return;
                        }
                        final String ext = post.imageExtension;
                        final String url = ApiModule.imgUrl(boardName, post.imageId, post.imageExtension);
                        switch (ext) {
                            case ".webm":
                                progress.setVisibility(View.GONE);
                                play.setVisibility(View.VISIBLE);
                                image.setOnClickListener(new OnClickListener() {
                                    @Override public void onClick(View v) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(Uri.parse(url), "video/webm");
                                    if(intent.resolveActivity(getContext().getPackageManager()) != null)
                                        getContext().startActivity(intent);
                                    else
                                        Util.showToast(getContext(), R.string.no_app);
                                    }
                                });
                                break;
                            default:
                                initImageCallback(ImgIdExt.from(post), bitmapInfo.bitmap, true, imageCallback);
                                // Resized Gifs don't animate apparently, that's the reason for the case analysis
                                Builders.IV.F<?> placeholder = ion.build(image);
                                if (result.getBitmapInfo() != null) {
                                    BitmapDrawable bd = new BitmapDrawable(getResources(), result.getBitmapInfo().bitmap);
                                    placeholder = placeholder.placeholder(bd);
                                }
                                if (".gif".equals(ext))
                                    placeholder.animateGif(AnimateGifMode.ANIMATE).smartSize(true);
                                else
                                    placeholder.resize(size[W], size[H]);
                                placeholder.load(url).withBitmapInfo().setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                                    @Override
                                    public void onCompleted(Exception e, final ImageViewBitmapInfo result) {
                                        BitmapInfo bitmapInfo = result.getBitmapInfo();
                                        if (e != null || bitmapInfo == null) {
                                            progress.setVisibility(View.GONE);
                                            return;
                                        }
                                        progress.setVisibility(View.GONE);
                                        initImageCallback(ImgIdExt.from(post), bitmapInfo.bitmap, false, imageCallback);
                                    }
                                });
                                break;
                        }
                    }
                });
        }
    }
}
