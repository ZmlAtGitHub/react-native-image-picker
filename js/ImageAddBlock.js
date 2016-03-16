/**
 * Created by lvbingru on 2/29/16.
 */

import {styles} from './ImageAddBlock.style.js'

import React, {InteractionManager, Component, PropTypes, View, Text, TouchableOpacity, Image} from 'react-native';
import Icon from 'react-native-vector-icons/FontAwesome';
import ImagePicker from './ImagePicker'

const propTypes = {
    list: PropTypes.array,
    onChanged : PropTypes.func,
}

const defaultProps = {
}

const PICKER_LIMIT = 9

export default class ImageAddBlock extends Component {
    constructor(props) {
        super(props);

        this.state = {
        };
    }

    render() {
        const {list} = this.props;
        const limit = PICKER_LIMIT - this.props.list.length;

        return (
            <View style={[styles.container]}>
                {
                    list && list.map((el,index)=>{
                        const uri = this.getUri(el)
                        return(
                            <TouchableOpacity
                                key = {index}
                                style = {styles.listViewItem}
                                onPress = {()=>{
                                }}
                            >
                                <Image
                                    style = {styles.img}
                                    source = {{uri}}
                                />
                                <TouchableOpacity
                                  style = {styles.btnDel}
                                  onPress = {()=>{
                                    this.onDelete(index);
                                  }}
                                >
                                    <Icon
                                      name="trash"
                                      style={styles.del}
                                      size={17}
                                      color={'#b4b7b9'}
                                    />
                                </TouchableOpacity>
                            </TouchableOpacity>
                        )
                    })
                }
                {
                    limit>0?
                      <TouchableOpacity
                        style = {[styles.listViewItem, styles.addItem]}
                        onPress={()=>{
                            this.props.navigator.push({params:{
                                onSelectFinished:this.onSelectFinished.bind(this),
                                limit:limit,
                            }, component: ImagePicker})
                        }}
                      >
                          <Icon
                            name="plus"
                            style={styles.txtAdd}
                          />
                      </TouchableOpacity>:
                      null
                }
            </View>
        );
    }

    getUri(el) {
        if (el.node) {
            return el.node.image.uri
        }
        else {
            return el
        }
    }

    onSelectFinished(list) {
        const r = [...this.props.list, ...list];
        const {onChanged} = this.props;
        onChanged && onChanged(r);
    }

    onDelete(index) {
        const r = [...this.props.list];
        r.splice(index,1);
        const {onChanged} = this.props;
        onChanged && onChanged(r);
    }
}

ImageAddBlock.propTypes = propTypes;
ImageAddBlock.defaultProps = defaultProps;
