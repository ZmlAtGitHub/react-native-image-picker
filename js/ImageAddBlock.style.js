/**
 * Created by lvbingru on 11/21/15.
 */

import {StyleSheet, Dimensions} from 'react-native';

const SCREEN_WIDTH = Dimensions.get('window').width;

const configFour = {
    paddingLeft : 7,
    margin : 3,
    size : (SCREEN_WIDTH - 7 * 2 - 3 * 2 * 4) / 4,
}


export var styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: 'white',
        flexWrap : 'wrap',
        flexDirection : 'row',
        paddingLeft : configFour.paddingLeft,
    },

    listViewItem : {
        width : configFour.size,
        height : configFour.size,
        marginHorizontal : configFour.margin,
        marginVertical : 4,
    },

    addItem :{
        justifyContent : 'center',
        alignItems :'center',
        borderWidth : 1,
        borderColor : '#d3d3d3',
    },

    txtAdd : {
        color: '#d3d3d3',
        fontSize: 17,
    },

    img : {
        width : configFour.size,
        height : configFour.size,
    },

    viewLabel :{
        alignItems : 'center',
        justifyContent : 'center',
        height : 20,
        bottom : 0,
        left : 0,
        right : 0,
        position: 'absolute',
    },

    txtLabel : {
        textAlign : 'center',
        fontSize : 13,
        color : 'green',
    },

    btnDel : {
        width: 20,
        height : 20,
        backgroundColor : 'transparent',
        position : 'absolute',
        top: 0,
        right : 0,
        justifyContent: 'center',
        alignItems: 'center',
    },

    del : {
    }
});