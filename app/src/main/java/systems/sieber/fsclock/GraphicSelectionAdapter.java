package systems.sieber.fsclock;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class GraphicSelectionAdapter extends ArrayAdapter<GraphicItem> {

    static GraphicItem[] CLOCK_FACES = {
            new GraphicItem(0, R.drawable.analog_classic_bg, R.string.classic_design),
            new GraphicItem(1, R.drawable.analog_number_bg, R.string.number_design),
            new GraphicItem(2, R.drawable.analog_square_bg, R.string.square_design),
            new GraphicItem(-1, null, R.string.custom_image)
    };
    static GraphicItem[] HOUR_HANDS = {
            new GraphicItem(0, R.drawable.analog_classic_h, R.string.classic_design),
            new GraphicItem(1, R.drawable.analog_rounded_h, R.string.rounded_design),
            new GraphicItem(2, R.drawable.analog_elegant_h, R.string.elegant_design),
            new GraphicItem(3, R.drawable.analog_outlined_h, R.string.outlined_design),
            new GraphicItem(4, R.drawable.analog_rustic_h, R.string.rustic_design),
            new GraphicItem(-1, null, R.string.custom_image)
    };
    static GraphicItem[] MINUTE_HANDS = {
            new GraphicItem(0, R.drawable.analog_classic_m, R.string.classic_design),
            new GraphicItem(1, R.drawable.analog_rounded_m, R.string.rounded_design),
            new GraphicItem(2, R.drawable.analog_elegant_m, R.string.elegant_design),
            new GraphicItem(3, R.drawable.analog_outlined_m, R.string.outlined_design),
            new GraphicItem(4, R.drawable.analog_rustic_m, R.string.rustic_design),
            new GraphicItem(-1, null, R.string.custom_image)
    };
    static GraphicItem[] SECOND_HANDS = {
            new GraphicItem(0, R.drawable.analog_classic_s, R.string.classic_design),
            new GraphicItem(4, R.drawable.analog_rustic_s, R.string.rustic_design),
            new GraphicItem(5, R.drawable.analog_dot_s, R.string.dot_design),
            new GraphicItem(-1, null, R.string.custom_image)
    };

    LayoutInflater flater;

    public GraphicSelectionAdapter(Activity context, int resourceId, List<GraphicItem> list) {
        super(context, resourceId, list);
        flater = context.getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if(convertView == null)
            convertView = View.inflate(getContext(), android.R.layout.simple_spinner_item, null);

        GraphicItem rowItem = getItem(position);
        ((TextView) convertView).setText( rowItem.mTextResourceId );
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        viewHolder holder;
        if(convertView == null) {
            holder = new viewHolder();
            flater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = flater.inflate(R.layout.item_graphic, null, false);

            holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
            holder.imageView = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (viewHolder) convertView.getTag();
        }

        GraphicItem rowItem = getItem(position);
        if(rowItem.mGraphicResourceId != null)
            holder.imageView.setImageResource(rowItem.mGraphicResourceId);
        holder.txtTitle.setText(rowItem.mTextResourceId);

        return convertView;
    }

    static private class viewHolder {
        TextView txtTitle;
        ImageView imageView;
    }

    public static GraphicItem getById(int id, GraphicItem[] gis) {
        for(GraphicItem gi : gis) {
            if(gi.mId == id) {
                return gi;
            }
        }
        return null;
    }

}

class GraphicItem {
    Integer mId;
    Integer mGraphicResourceId;
    int mTextResourceId;

    GraphicItem(Integer id, Integer graphicResourceId, int textResourceId) {
        mId = id;
        mGraphicResourceId = graphicResourceId;
        mTextResourceId = textResourceId;
    }
}
